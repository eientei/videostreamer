package main

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
	"os"
	"time"

	"./db"
	"./mp4"
	"./rtmp"
	"./web"
)

const (
	UnknownRole = iota
	PublisherRole
	SubscriberRole
)

const (
	UnknownEvent = iota
	PublishedEvent
	UnpublishedEvent
	SubscribedEvent
	UnsubscribedEvent
	InitEvent
)

type Eventeer struct {
	Client        web.EventClient
	Subscriptions []string
	User          *db.User
	Ip            string
}

type Event struct {
	Source string
	Type   uint8
	Detail string
}

type Stream struct {
	Path          string
	Name          string
	Metadata      *rtmp.Metadata
	AudioInit     []byte
	VideoInit     []byte
	Clients       []web.Client
	AudioBuffer   chan *rtmp.TimestampBuf
	VideoBuffer   chan *rtmp.TimestampBuf
	ContainerInit []byte
	Muxer         *mp4.Muxer
}

func (stream *Stream) MuxHandle(event *mp4.MuxEvent) {
	for _, c := range stream.Clients {
		if c.Sequence() == 0 {
			//t := uint64(time.Now().UnixNano()) / uint64(time.Millisecond)
			c.Init(0, 0)
			vfirst := 0
			tskip := uint32(0)
			for i, c := range event.VideoBuffer {
				if c.SliceType == 7 {
					vfirst = i
					break
				}
				tskip += c.Sample.Duration
			}
			aacc := uint32(0)
			afirst := 0
			for i, c := range event.AudioBuffer {
				if aacc >= tskip {
					afirst = i
					break
				}
				aacc += c.Sample.Duration
			}
			if len(event.VideoBuffer[vfirst:]) > 0 {
				data, atime, vtime := stream.Muxer.RenderEvent(&mp4.MuxEvent{AudioBuffer: event.AudioBuffer[afirst:], VideoBuffer: event.VideoBuffer[vfirst:]}, c.Sequence(), c.Atime(), c.Vtime())
				c.Send(data)
				c.Advance(1, uint64(atime), uint64(vtime))
			}
		} else {
			data, atime, vtime := stream.Muxer.RenderEvent(event, c.Sequence(), c.Atime(), c.Vtime())
			c.Send(data)
			c.Advance(1, uint64(atime), uint64(vtime))
		}
	}
}

func (stream *Stream) Close() {
	for _, c := range stream.Clients {
		c.Close()
	}
}

type RtmpClient struct {
	Id     rtmp.ID
	Role   uint8
	Stream *Stream
}

type Coordinator struct {
	Config      *Config
	WebServer   *web.Server
	RtmpServer  *rtmp.Server
	RtmpClients map[rtmp.ID]*RtmpClient
	Streams     map[string]*Stream
	Events      chan *Event
	Eventeers   []*Eventeer
}

func (coordinator *Coordinator) ClientConnect(client web.Client, path string, name string) bool {
	if stream, ok := coordinator.Streams[path+"/"+name]; !ok {
		return false
	} else {
		if stream.Metadata == nil {
			return false
		}
		stream.Clients = append(stream.Clients, client)
		client.Send(stream.ContainerInit)
	}

	coordinator.Events <- &Event{
		Source: client.Source(),
		Type:   SubscribedEvent,
		Detail: path + "/" + name,
	}

	return true
}

func (coordinator *Coordinator) ClientDisconnect(client web.Client, path string, name string) {
	if stream, ok := coordinator.Streams[path+"/"+name]; ok {
		for i, c := range stream.Clients {
			if c == client {
				stream.Clients = append(stream.Clients[:i], stream.Clients[i+1:]...)
				break
			}
		}
	}

	coordinator.Events <- &Event{
		Source: client.Source(),
		Type:   UnsubscribedEvent,
		Detail: path + "/" + name,
	}
}

type RecaptchaResponse struct {
	Success     bool      `json:"success"`
	ChallengeTS time.Time `json:"challenge_ts"`
	Hostname    string    `json:"hostname"`
	ErrorCodes  []string  `json:"error-codes"`
}

func (coordinator *Coordinator) ProcessEvents(eventeer *Eventeer) {
	for {
		msg := eventeer.Client.Read()
		switch msg.Type() {
		case web.Signup:
			signup := msg.(*web.SignupMessage)
			if signup.Ip != "127.0.0.1" {
				res := &RecaptchaResponse{}
				if r, err := http.PostForm("https://www.google.com/recaptcha/api/siteverify", url.Values{"secret": {coordinator.Config.Web.Recaptcha}, "remoteip": {signup.Ip}, "response": {signup.Captcha}}); err != nil {
					eventeer.Client.Send(&web.ErrorMessage{"Invalid signup"})
					continue
				} else {
					defer r.Body.Close()
					if body, err := ioutil.ReadAll(r.Body); err != nil {
						eventeer.Client.Send(&web.ErrorMessage{"Invalid signup"})
						continue
					} else {
						fmt.Println(string(body))
						if err := json.Unmarshal(body, res); err != nil {
							eventeer.Client.Send(&web.ErrorMessage{"Invalid signup"})
							continue
						}
					}
				}
				if !res.Success {
					eventeer.Client.Send(&web.ErrorMessage{"Invalid captcha"})
					continue
				}
			}

			if len(signup.Username) < 3 || len(signup.Username) > 64 {
				eventeer.Client.Send(&web.ErrorMessage{"Username must be of 3..64 symbols in length"})
				continue
			}

			if len(signup.Password) < 3 || len(signup.Password) > 64 {
				eventeer.Client.Send(&web.ErrorMessage{"Password must be of 3..64 symbols in length"})
				continue
			}

			if signup.Password != signup.PasswordRepeat {
				eventeer.Client.Send(&web.ErrorMessage{"Passwords must match"})
				continue
			}

			if db.GetUserByName(signup.Username) != nil {
				eventeer.Client.Send(&web.ErrorMessage{"User with such name exists"})
				continue
			}

			if db.GetUserByEmail(signup.Email) != nil {
				eventeer.Client.Send(&web.ErrorMessage{"User with such email exists"})
				continue
			}

			db.CreateUser(db.User{
				Username: signup.Username,
				Email:    signup.Email,
				Password: signup.Password,
			})
			eventeer.Client.Send(&web.StatusMessage{"Signup success"})
		case web.Auth:
			auth := msg.(*web.AuthMessage)
			eventeer.Ip = auth.Ip
			getAnon := func() *web.UserDetailsMessage {
				user := db.GetUserByEmail(auth.Ip)
				if user == nil {
					user = &db.User{
						Username: "anonymous",
						Email:    auth.Ip,
						Password: "",
					}
					db.CreateUser(*user)
				}
				gsum := md5.Sum([]byte(user.Email))
				gravatar := hex.EncodeToString(gsum[:])
				eventeer.User = user
				anon := &web.UserDetailsMessage{
					Username: user.Username,
					Email:    user.Email,
					Gravatar: gravatar,
				}
				return anon
			}

			if auth.Username == "anonymous" {
				eventeer.Client.Send(getAnon())
			} else {
				fmt.Println(auth)
				user := db.GetUserByName(auth.Username)
				fmt.Println(user)
				if user == nil {
					eventeer.Client.Send(getAnon())
					continue
				}
				if user.Password != auth.Password {
					eventeer.Client.Send(getAnon())
					continue
				}
				dbstreams := db.GetStreamsByOwner(user.Id)
				streams := make([]*web.Stream, 0)
				for _, s := range dbstreams {
					streams = append(streams, &web.Stream{
						Name:    s.Name,
						Title:   s.Title,
						Key:     s.Key,
						Logourl: s.Logourl,
					})
				}
				notifications := make([]string, 0)
				subs := db.GetSubscriptionsByUser(user.Id)
				for _, s := range subs {
					notifications = append(notifications, user.Username+"/"+s)
				}

				gsum := md5.Sum([]byte(user.Email))
				gravatar := hex.EncodeToString(gsum[:])
				eventeer.User = user
				eventeer.Client.Send(&web.UserDetailsMessage{
					Username:      user.Username,
					Email:         user.Email,
					Gravatar:      gravatar,
					Streams:       streams,
					Notifications: notifications,
				})
			}

		case web.Logout:
			user := db.GetUserByEmail(eventeer.Ip)
			if user == nil {
				user = &db.User{
					Username: "anonymous",
					Email:    eventeer.Ip,
					Password: "",
				}
				db.CreateUser(*user)
			}
			gsum := md5.Sum([]byte(user.Email))
			gravatar := hex.EncodeToString(gsum[:])
			eventeer.User = user
			eventeer.Client.Send(&web.UserDetailsMessage{
				Username: user.Username,
				Email:    user.Email,
				Gravatar: gravatar,
			})
		case web.Disconnect:
			fmt.Println("disconnect")
			return
		}
	}
}

func (coordinator *Coordinator) EventeerConnect(client web.EventClient) {
	evt := &Eventeer{Client: client}
	coordinator.Eventeers = append(coordinator.Eventeers, evt)
	go coordinator.ProcessEvents(evt)
}

func (coordinator *Coordinator) EventeerDisconnect(client web.EventClient) {
	for i, c := range coordinator.Eventeers {
		if c.Client == client {
			coordinator.Eventeers = append(coordinator.Eventeers[:i], coordinator.Eventeers[i+1:]...)
		}
	}
}

func (coordinator *Coordinator) ConnectEvent(client rtmp.ID) {
	c := &RtmpClient{
		Id: client,
	}
	coordinator.RtmpClients[client] = c
}

func (coordinator *Coordinator) DisconnectEvent(client rtmp.ID) {
	switch coordinator.RtmpClients[client].Role {
	case PublisherRole:
		coordinator.Events <- &Event{
			Source: client.String(),
			Type:   UnpublishedEvent,
			Detail: coordinator.RtmpClients[client].Stream.Path + "/" + coordinator.RtmpClients[client].Stream.Name,
		}
		close(coordinator.RtmpClients[client].Stream.AudioBuffer)
		close(coordinator.RtmpClients[client].Stream.VideoBuffer)
		delete(coordinator.Streams, coordinator.RtmpClients[client].Stream.Path+"/"+coordinator.RtmpClients[client].Stream.Name)
	case SubscriberRole:
		coordinator.Events <- &Event{
			Source: client.String(),
			Type:   UnsubscribedEvent,
			Detail: coordinator.RtmpClients[client].Stream.Path + "/" + coordinator.RtmpClients[client].Stream.Name,
		}
	}
	delete(coordinator.RtmpClients, client)
}

func (coordinator *Coordinator) PublishEvent(client rtmp.ID, path string, stream string) bool {
	if _, ok := coordinator.Streams[path+"/"+stream]; ok {
		return false
	} else {
		s := &Stream{
			Path:        path,
			Name:        stream,
			AudioBuffer: make(chan *rtmp.TimestampBuf),
			VideoBuffer: make(chan *rtmp.TimestampBuf),
		}
		coordinator.Streams[path+"/"+stream] = s
		coordinator.RtmpClients[client].Role = PublisherRole
		coordinator.RtmpClients[client].Stream = s

		coordinator.Events <- &Event{
			Source: client.String(),
			Type:   PublishedEvent,
			Detail: path + "/" + stream,
		}
		return true
	}
}

func (coordinator *Coordinator) SubscribeEvent(client rtmp.ID, path string, stream string) bool {
	return false
}

func (coordinator *Coordinator) InitEvent(client rtmp.ID, data *rtmp.Metadata, audio []byte, video []byte) bool {
	coordinator.RtmpClients[client].Stream.Metadata = data
	coordinator.RtmpClients[client].Stream.AudioInit = audio
	coordinator.RtmpClients[client].Stream.VideoInit = video

	if !coordinator.Remux(coordinator.RtmpClients[client].Stream) {
		return false
	}

	coordinator.Events <- &Event{
		Source: client.String(),
		Type:   InitEvent,
		Detail: coordinator.RtmpClients[client].Stream.Path + "/" + coordinator.RtmpClients[client].Stream.Name,
	}
	return true
}

func (coordinator *Coordinator) AudioEvent(client rtmp.ID, data *rtmp.TimestampBuf) bool {
	coordinator.RtmpClients[client].Stream.AudioBuffer <- data
	return true
}

func (coordinator *Coordinator) VideoEvent(client rtmp.ID, data *rtmp.TimestampBuf) bool {
	coordinator.RtmpClients[client].Stream.VideoBuffer <- data
	return true
}

func Dispatch(stream *Stream) {
	defer stream.Close()
	for {
		select {
		case msg, ok := <-stream.AudioBuffer:
			if !ok {
				return
			}

			ptr := 0
			format := msg.Data[ptr] >> 4
			ptr += 1
			if format != 10 {
				return
			}
			if (msg.Data[ptr]) == 0 {
				continue
			}
			ptr += 1

			stream.Muxer.Audio(msg.Data[ptr:])
		case msg, ok := <-stream.VideoBuffer:
			if !ok {
				return
			}
			ptr := 0
			frame := (msg.Data[ptr] >> 4) & 0xF
			codec := msg.Data[ptr] & 0xF
			ptr += 1
			avctype := -1
			if codec == 7 {
				avctype = int(msg.Data[ptr])
				ptr += 1
				ptr += 3
			}
			if frame == 5 {
				continue
			} else {
				switch codec {
				case 7:
					switch avctype {
					case 0:
						continue
					case 1:
						stream.Muxer.Video(msg.Data[ptr:])
					default:
						return
					}
				default:
					return
				}
			}
		}
	}
}

func (coordinator *Coordinator) Remux(stream *Stream) bool {
	stream.Muxer = mp4.NewMuxer(coordinator.Config.Mp4)

	aptr := 0
	format := stream.AudioInit[aptr] >> 4
	aptr += 1
	if format != 10 {
		return false
	}
	aptr += 1

	vptr := 0
	frame := (stream.VideoInit[vptr] >> 4) & 0xF
	codec := stream.VideoInit[vptr] & 0xF
	vptr += 1
	avctype := -1
	if codec == 7 {
		avctype = int(stream.VideoInit[vptr])
		vptr += 1
		vptr += 3
	}
	if frame == 5 {
		return false
	} else {
		switch codec {
		case 7:
			switch avctype {
			case 0:
			case 1:
				return false
			default:
				return false
			}
		default:
			return false
		}
	}

	stream.ContainerInit = stream.Muxer.Init(stream.Metadata.Width, stream.Metadata.Height, stream.Metadata.FrameRate, stream.Metadata.AudioRate, stream.AudioInit[aptr:], stream.VideoInit[vptr:])
	if len(stream.ContainerInit) == 0 {
		return false
	}
	stream.Muxer.Subscribe(stream)
	go Dispatch(stream)
	return true
}

func (coordinator *Coordinator) Coordinate() {
	logger := log.New(os.Stdout, "", log.LstdFlags)
	for event := range coordinator.Events {
		switch event.Type {
		case PublishedEvent:
			logger.Println("[", event.Detail, "] Published", event.Source)
		case UnpublishedEvent:
			logger.Println("[", event.Detail, "] Unpublished", event.Source)
		case SubscribedEvent:
			logger.Println("[", event.Detail, "] Subscribed", event.Source)
		case UnsubscribedEvent:
			logger.Println("[", event.Detail, "] Unsubscribed", event.Source)
		case InitEvent:
			logger.Println("[", event.Detail, "] Ready")
			for _, c := range coordinator.Eventeers {
				for _, s := range c.Subscriptions {
					if s == event.Detail {
						c.Client.Send(&web.PublishedMessage{Stream: event.Detail})
						break
					}
				}
			}
		}
	}
}
