package main

import (
	"crypto/md5"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"math/rand"
	"net/http"
	"net/url"
	"regexp"
	"sort"
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

type Stream struct {
	Owner         string
	Name          string
	Metadata      *rtmp.Metadata
	AudioInit     []byte
	VideoInit     []byte
	Clients       []web.Client
	WebClients    []*WebClient
	AudioBuffer   chan *rtmp.TimestampBuf
	VideoBuffer   chan *rtmp.TimestampBuf
	ContainerInit []byte
	Muxer         *mp4.Muxer
	Closed        bool
	Coordinator   *Coordinator
}

type WebClient struct {
	Stream        *Stream
	Client        web.EventClient
	Notifications []string
	Coordinator   *Coordinator
	User          *db.User
}

type RtmpClient struct {
	Id     rtmp.ID
	Role   uint8
	Stream *Stream
}

type Coordinator struct {
	Config      *Config
	WebClients  []*WebClient
	RtmpClients map[rtmp.ID]*RtmpClient
	Streams     map[string]*Stream
}

func (stream *Stream) MuxHandle(event *mp4.MuxEvent) {
	for _, c := range stream.Clients {
		if c.Sequence() == 0 {
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
	if stream.Closed {
		return
	}
	stream.Closed = true
	close(stream.AudioBuffer)
	close(stream.VideoBuffer)
	stream.ContainerInit = make([]byte, 0)
	stream.Metadata = nil
	for _, c := range stream.Clients {
		c.Close()
	}
	stream.Clients = nil
}

func MakeStreamName(path string, name string) string {
	n := path
	if len(name) > 0 {
		n += "/" + name
	}
	return n
}

func (coordinator *Coordinator) ClientOk(path string, name string) bool {
	if stream, ok := coordinator.Streams[MakeStreamName(path, name)]; !ok {
		return false
	} else {
		if stream.Closed {
			return false
		}
		if stream.Metadata == nil {
			return false
		}
		if len(stream.ContainerInit) == 0 {
			return false
		}
	}

	return true
}

func (coordinator *Coordinator) ClientConnect(client web.Client, path string, name string) {
	stream := coordinator.Streams[MakeStreamName(path, name)]
	client.Send(stream.ContainerInit)
	stream.Clients = append(stream.Clients, client)

	str := db.GetStreamByOwnerNameAndName(path, name)
	user := db.GetUserById(str.Id)
	for _, c := range stream.WebClients {
		c.Client.Send(&web.StreamInfoMessage{Stream: MakeStreamInfo(user, str, stream)})
	}
}

func (coordinator *Coordinator) ClientDisconnect(client web.Client, path string, name string) {
	if stream, ok := coordinator.Streams[MakeStreamName(path, name)]; ok {
		for i, c := range stream.Clients {
			if c == client {
				stream.Clients = append(stream.Clients[:i], stream.Clients[i+1:]...)
				break
			}
		}

		str := db.GetStreamByOwnerNameAndName(path, name)
		user := db.GetUserById(str.Id)
		for _, c := range stream.WebClients {
			c.Client.Send(&web.StreamInfoMessage{Stream: MakeStreamInfo(user, str, stream)})
		}
	}
}

func (coordinator *Coordinator) EventeerConnect(client web.EventClient) {
	evt := &WebClient{Client: client, Coordinator: coordinator, User: &db.User{
		Id:       0,
		Username: "anonymous",
		Email:    "",
		Password: "",
	}}
	coordinator.WebClients = append(coordinator.WebClients, evt)
	go coordinator.ProcessEvents(evt)
}

func (coordinator *Coordinator) EventeerDisconnect(client web.EventClient) {
	for i, c := range coordinator.WebClients {
		if c.Client == client {
			coordinator.WebClients = append(coordinator.WebClients[:i], coordinator.WebClients[i+1:]...)
			if c.Stream != nil {
				for n, s := range c.Stream.WebClients {
					if s.Client == client {
						c.Stream.WebClients = append(c.Stream.WebClients[:n], c.Stream.WebClients[n+1:]...)
						break
					}
				}
			}
			break
		}
	}
}

func (coordinator *Coordinator) ConnectEvent(client rtmp.ID) {
	coordinator.RtmpClients[client] = &RtmpClient{
		Id: client,
	}
}

func (coordinator *Coordinator) DisconnectEvent(client rtmp.ID) {
	s, ok := coordinator.RtmpClients[client]
	if !ok {
		return
	}
	switch s.Role {
	case PublisherRole:
		s.Stream.Close()
	case SubscriberRole:
	}
	delete(coordinator.RtmpClients, client)
}

func (coordinator *Coordinator) PublishEvent(client rtmp.ID, path string, stream string) bool {
	str := db.GetStreamByOwnerNameAndKey(path, stream)
	if str == nil {
		return false
	}
	if s, ok := coordinator.Streams[MakeStreamName(path, str.Name)]; ok && !s.Closed {
		return false
	} else {
		if !ok {
			owner := db.GetUserById(str.Id)
			s = &Stream{
				AudioBuffer: make(chan *rtmp.TimestampBuf),
				VideoBuffer: make(chan *rtmp.TimestampBuf),
				Owner:       owner.Username,
				Name:        str.Name,
				Coordinator: coordinator,
			}
			coordinator.Streams[MakeStreamName(path, str.Name)] = s
		} else {
			s.Closed = false
			s.AudioBuffer = make(chan *rtmp.TimestampBuf)
			s.VideoBuffer = make(chan *rtmp.TimestampBuf)
		}
		coordinator.RtmpClients[client].Role = PublisherRole
		coordinator.RtmpClients[client].Stream = s
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

	stream := coordinator.RtmpClients[client].Stream
	user := db.GetUserByName(stream.Owner)

	str := db.GetStreamByOwnerNameAndName(stream.Owner, stream.Name)

	n := MakeStreamName(stream.Owner, stream.Name)

	for _, c := range coordinator.WebClients {
		for _, s := range c.Notifications {
			if s == n {
				c.Client.Send(&web.StreamPublishedMessage{Stream: MakeStreamInfo(user, str, stream)})
				break
			}
		}
	}

	return true
}

func (coordinator *Coordinator) AudioEvent(client rtmp.ID, data *rtmp.TimestampBuf) bool {
	s, ok := coordinator.RtmpClients[client]
	if !ok {
		return false
	}
	if s.Stream.Closed {
		return false
	}
	s.Stream.AudioBuffer <- data
	return true
}

func (coordinator *Coordinator) VideoEvent(client rtmp.ID, data *rtmp.TimestampBuf) bool {
	s, ok := coordinator.RtmpClients[client]
	if !ok {
		return false
	}
	if s.Stream.Closed {
		return false
	}
	s.Stream.VideoBuffer <- data
	return true
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

func (coordinator *Coordinator) ProcessEvents(eventeer *WebClient) {
	for {
		msg := eventeer.Client.Read()
		if msg == nil {
			break
		}
		switch msg.Type() {
		case web.UserSignup:
			eventeer.UserSignup(msg.(*web.UserSignupMessage))
		case web.UserLogin:
			eventeer.UserLogin(msg.(*web.UserLoginMessage))
		case web.UserLogout:
			eventeer.UserLogout(msg.(*web.UserLogoutMessage))
		case web.UserInfoUpdate:
			eventeer.UserInfoUpdate(msg.(*web.UserInfoUpdateMessage))
		case web.StreamInfoUpdate:
			eventeer.StreamInfoUpdate(msg.(*web.StreamInfoUpdateMessage))
		case web.StreamKeyUpdate:
			eventeer.StreamKeyUpdate(msg.(*web.StreamKeyUpdateMessage))
		case web.StreamPrivatedUpdate:
			eventeer.StreamPrivatedUpdate(msg.(*web.StreamPrivatedUpdateMessage))
		case web.StreamDelete:
			eventeer.StreamDelete(msg.(*web.StreamDeleteMessage))
		case web.StreamAdd:
			eventeer.StreamAdd(msg.(*web.StreamAddMessage))
		case web.StreamSubscribe:
			eventeer.StreamSubscribe(msg.(*web.StreamSubscribeMessage))
		case web.StreamUnsubscribe:
			eventeer.StreamUnsubscribe(msg.(*web.StreamUnsubscribeMessage))
		case web.StreamList:
			eventeer.StreamList(msg.(*web.StreamListMessage))
		case web.StreamInfoReq:
			eventeer.StreamInfoReq(msg.(*web.StreamInfoReqMessage))
		case web.MessageSend:
			eventeer.MessageSend(msg.(*web.MessageSendMessage))
		case web.MessageEdit:
			eventeer.MessageEdit(msg.(*web.MessageEditMessage))
		case web.MessageDelete:
			eventeer.MessageDelete(msg.(*web.MessageDeleteMessage))
		case web.MessageHistory:
			eventeer.MessageHistory(msg.(*web.MessageHistoryMessage))
		}
	}
}

type RecaptchaResponse struct {
	Success     bool      `json:"success"`
	ChallengeTS time.Time `json:"challenge_ts"`
	Hostname    string    `json:"hostname"`
	ErrorCodes  []string  `json:"error-codes"`
}

func GenerateKey() string {
	t := time.Now().UnixNano()
	b := make([]byte, 20)
	WriteB64(b, uint64(t))
	rand.Read(b[4:])
	sum := sha1.Sum(b)
	return hex.EncodeToString(sum[:])
}

func MakeGravatar(eventeer *WebClient) string {
	if eventeer.User.Username != "anonymous" {
		gsum := md5.Sum([]byte(eventeer.User.Email))
		return hex.EncodeToString(gsum[:])
	} else {
		ip := eventeer.Client.Ip()
		for i, c := range ip {
			if c == ':' {
				ip = ip[:i]
				break
			}
		}
		gsum := md5.Sum([]byte(ip))
		return hex.EncodeToString(gsum[:])
	}
}

type UserDetailsStreamsArr []*web.UserDetailsStreams

func (s UserDetailsStreamsArr) Len() int {
	return len(s)
}

func (s UserDetailsStreamsArr) Less(i, j int) bool {
	return s[i].Name < s[j].Name
}

func (s UserDetailsStreamsArr) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}

func MakeStreamInfo(owner *db.User, stream *db.Stream, running *Stream) *web.UserDetailsStreams {
	gsum := md5.Sum([]byte(owner.Email))
	gravatar := hex.EncodeToString(gsum[:])

	clients := 0
	users := make([]string, 0)
	if running != nil {
		for _, c := range running.WebClients {
			users = append(users, MakeGravatar(c))
		}
		clients = len(running.Clients)
	}
	oldest := db.GetOldestMessage(stream.Id)
	return &web.UserDetailsStreams{
		Id:       stream.Id,
		Gravatar: gravatar,
		Owner:    owner.Username,
		Name:     stream.Name,
		Title:    stream.Title,
		Logourl:  stream.Logourl,
		Key:      stream.Key,
		Privated: stream.Privated,
		Clients:  clients,
		Users:    users,
		Oldest:   oldest,
	}
}

func MakeUserInfo(eventeer *WebClient) *web.UserDetailsMessage {
	gravatar := MakeGravatar(eventeer)

	dbstreams := db.GetStreamsByOwner(eventeer.User.Id)
	streams := make([]*web.UserDetailsStreams, 0)
	for _, s := range dbstreams {
		streams = append(streams, MakeStreamInfo(eventeer.User, s, eventeer.Coordinator.Streams[MakeStreamName(eventeer.User.Username, s.Name)]))
	}
	sort.Sort(UserDetailsStreamsArr(streams))
	return &web.UserDetailsMessage{
		Name:          eventeer.User.Username,
		Email:         eventeer.User.Email,
		Gravatar:      gravatar,
		Streams:       streams,
		Notifications: eventeer.Notifications,
	}
}

func (webclient *WebClient) UserSignup(msg *web.UserSignupMessage) {
	if len(msg.Username) < 3 || len(msg.Username) > 64 {
		webclient.Client.Send(&web.SignupErrorMessage{"Username must be of 3..64 symbols in length"})
		return
	}
	if len(msg.Email) < 3 || len(msg.Email) > 64 {
		webclient.Client.Send(&web.SignupErrorMessage{"Email must be of 3..64 symbols in length"})
		return
	}
	if len(msg.Password) < 3 || len(msg.Password) > 64 {
		webclient.Client.Send(&web.SignupErrorMessage{"Password must be of 3..64 symbols in length"})
		return
	}
	if b, _ := regexp.Match("^[a-zA-Z0-9]+$", []byte(msg.Username)); !b {
		webclient.Client.Send(&web.SignupErrorMessage{"Username must be alphanumeric"})
		return
	}
	ip := webclient.Client.Ip()
	for i, c := range ip {
		if c == ':' {
			ip = ip[:i]
			break
		}
	}
	res := &RecaptchaResponse{}
	if r, err := http.PostForm("https://www.google.com/recaptcha/api/siteverify", url.Values{"secret": {webclient.Coordinator.Config.Web.Recaptcha}, "remoteip": {ip}, "response": {msg.Captcha}}); err != nil {
		webclient.Client.Send(&web.SignupErrorMessage{"Invalid signup"})
		return
	} else {
		defer r.Body.Close()
		if body, err := ioutil.ReadAll(r.Body); err != nil {
			webclient.Client.Send(&web.SignupErrorMessage{"Invalid signup"})
			return
		} else {
			if err := json.Unmarshal(body, res); err != nil {
				webclient.Client.Send(&web.SignupErrorMessage{"Invalid signup"})
				return
			}
		}
	}
	if !res.Success {
		webclient.Client.Send(&web.SignupErrorMessage{"Invalid captcha"})
		return
	}

	if db.GetUserByName(msg.Username) != nil {
		webclient.Client.Send(&web.SignupErrorMessage{"Name already taken"})
		return
	}

	if db.GetUserByEmail(msg.Email) != nil {
		webclient.Client.Send(&web.SignupErrorMessage{"Email already taken"})
		return
	}

	uid := db.CreateUser(db.User{
		Username: msg.Username,
		Email:    msg.Email,
		Password: msg.Password,
	})
	db.CreateStream(db.Stream{
		Name:     "",
		Title:    "Changeme",
		Owner:    uid,
		Key:      GenerateKey(),
		Privated: false,
		Logourl:  "",
	})
	for _, n := range webclient.Notifications {
		owner := n
		name := ""
		for i, c := range n {
			if c == '/' {
				owner = n[:i]
				name = n[i+1:]
				break
			}
		}
		s := db.GetStreamByOwnerNameAndName(owner, name)
		db.Subscribe(uid, s.Id)
	}

	webclient.User = db.GetUserByName(msg.Username)
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) UserLogin(msg *web.UserLoginMessage) {
	if msg.Username == "anonymous" {
		webclient.User = &db.User{
			Id:       0,
			Username: "anonymous",
			Email:    "",
			Password: "",
		}
		webclient.Client.Send(MakeUserInfo(webclient))
		return
	}
	user := db.GetUserByName(msg.Username)
	if user == nil {
		webclient.Client.Send(&web.LoginErrorMessage{"No such user"})
		return
	}
	if user.Password != msg.Password {
		webclient.Client.Send(&web.LoginErrorMessage{"Invalid credentials"})
		return
	}
	webclient.User = user
	webclient.Notifications = make([]string, 0)
	subs := db.GetSubscriptionsByUser(webclient.User.Id)
	for _, s := range subs {
		webclient.Notifications = append(webclient.Notifications, s)
	}
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) UserLogout(msg *web.UserLogoutMessage) {
	webclient.User = &db.User{
		Id:       0,
		Username: "anonymous",
		Email:    "",
		Password: "",
	}
	webclient.Notifications = make([]string, 0)
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) UserInfoUpdate(msg *web.UserInfoUpdateMessage) {
	if webclient.User.Username == "anonymous" {
		webclient.Client.Send(&web.ProfileErrorMessage{"Not logged in"})
		return
	}

	if webclient.User.Password != msg.Password && msg.Password != "" {
		db.UpdateUserPassword(webclient.User.Id, msg.Password)
		webclient.User.Password = msg.Password
	}

	if webclient.User.Email != msg.Email {
		usr := db.GetUserByEmail(msg.Email)
		if usr != nil {
			if usr.Id != webclient.User.Id {
				webclient.Client.Send(&web.ProfileErrorMessage{"Email already taken"})
			}
			return
		}
		db.UpdateUserEmail(webclient.User.Id, msg.Email)
		webclient.User.Email = msg.Email
	}

	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamInfoUpdate(msg *web.StreamInfoUpdateMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.StreamErrorMessage{"Not such stream"})
		return
	}
	if stream.Owner != webclient.User.Id {
		webclient.Client.Send(&web.StreamErrorMessage{"Stream not owned"})
		return
	}

	if stream.Title != msg.Title {
		db.UpdateStreamTitle(msg.Id, msg.Title)
		stream.Title = msg.Title
	}

	if stream.Logourl != msg.Logourl {
		db.UpdateStreamLogourl(msg.Id, msg.Logourl)
		stream.Logourl = msg.Logourl
	}

	if stream.Name != msg.Name {
		if b, _ := regexp.Match("^[a-zA-Z0-9]*$", []byte(msg.Name)); !b {
			webclient.Client.Send(&web.StreamErrorMessage{"Name must be alphanumeric"})
			return
		}
		str := db.GetStreamByOwnerAndName(webclient.User.Id, msg.Name)
		if str != nil {
			if str.Id != msg.Id {
				webclient.Client.Send(&web.StreamErrorMessage{"Stream name already exists"})
			}
			return
		}
		db.UpdateStreamName(msg.Id, msg.Name)

		n := MakeStreamName(webclient.User.Username, stream.Name)

		running := webclient.Coordinator.Streams[n]

		for _, c := range webclient.Coordinator.WebClients {
			for i, nm := range c.Notifications {
				if nm == n {
					c.Notifications[i] = MakeStreamName(webclient.User.Username, msg.Name)
					break
				}
			}
		}

		if running != nil {
			for _, c := range running.WebClients {
				c.Client.Send(&web.StreamRedirectMessage{Owner: webclient.User.Username, Stream: msg.Name})
			}
			delete(webclient.Coordinator.Streams, n)
			running.Close()
		}

		webclient.Client.Send(MakeUserInfo(webclient))
		return
	}

	running := webclient.Coordinator.Streams[MakeStreamName(webclient.User.Username, stream.Name)]

	if running != nil {
		for _, c := range running.WebClients {
			c.Client.Send(&web.StreamInfoMessage{Stream: MakeStreamInfo(webclient.User, stream, running)})
		}
	}

	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamKeyUpdate(msg *web.StreamKeyUpdateMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.StreamErrorMessage{"Not such stream"})
		return
	}
	if stream.Owner != webclient.User.Id {
		webclient.Client.Send(&web.StreamErrorMessage{"Stream not owned"})
		return
	}

	key := GenerateKey()
	db.UpdateStreamKey(msg.Id, key)
	webclient.Client.Send(MakeUserInfo(webclient))

	running := webclient.Coordinator.Streams[MakeStreamName(webclient.User.Username, stream.Name)]

	if running != nil {
		running.Close()
	}
}

func (webclient *WebClient) StreamPrivatedUpdate(msg *web.StreamPrivatedUpdateMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.StreamErrorMessage{"Not such stream"})
		return
	}
	if stream.Owner != webclient.User.Id {
		webclient.Client.Send(&web.StreamErrorMessage{"Stream not owned"})
		return
	}

	db.UpdateStreamPrivated(msg.Id, msg.Privated)
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamDelete(msg *web.StreamDeleteMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.StreamErrorMessage{"Not such stream"})
		return
	}
	if stream.Owner != webclient.User.Id {
		webclient.Client.Send(&web.StreamErrorMessage{"Stream not owned"})
		return
	}

	db.DeleteStreamById(msg.Id)
	webclient.Client.Send(MakeUserInfo(webclient))

	n := MakeStreamName(webclient.User.Username, stream.Name)

	running := webclient.Coordinator.Streams[n]

	for _, c := range webclient.Coordinator.WebClients {
		for i, nm := range c.Notifications {
			if nm == n {
				c.Notifications = append(c.Notifications[:i], c.Notifications[i+1:]...)
				break
			}
		}
	}

	if running != nil {
		for _, c := range running.WebClients {
			c.Client.Send(&web.StreamRedirectMessage{Owner: "", Stream: ""})
		}
		delete(webclient.Coordinator.Streams, MakeStreamName(webclient.User.Username, stream.Name))
		running.Close()
	}
}

func (webclient *WebClient) StreamAdd(msg *web.StreamAddMessage) {
	name := ""
	for i := 0; ; i++ {
		name = fmt.Sprintf("stream%02d", i)
		stream := db.GetStreamByOwnerAndName(webclient.User.Id, name)
		if stream == nil {
			break
		}
	}
	db.CreateStream(db.Stream{
		Name:     name,
		Title:    "",
		Owner:    webclient.User.Id,
		Key:      GenerateKey(),
		Logourl:  "",
		Privated: false,
	})
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamSubscribe(msg *web.StreamSubscribeMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.SubErrorMessage{"Not such stream"})
		return
	}
	owner := db.GetUserById(stream.Owner)
	webclient.Notifications = append(webclient.Notifications, MakeStreamName(owner.Username, stream.Name))
	if webclient.User.Username != "anonymous" {
		db.Subscribe(webclient.User.Id, stream.Id)
	}
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamUnsubscribe(msg *web.StreamUnsubscribeMessage) {
	stream := db.GetStreamById(msg.Id)
	if stream == nil {
		webclient.Client.Send(&web.SubErrorMessage{"Not such stream"})
		return
	}
	owner := db.GetUserById(stream.Owner)
	n := MakeStreamName(owner.Username, stream.Name)
	for i, nm := range webclient.Notifications {
		if nm == n {
			webclient.Notifications = append(webclient.Notifications[:i], webclient.Notifications[i+1:]...)
			break
		}
	}

	if webclient.User.Username != "anonymous" {
		db.Unsubscribe(webclient.User.Id, stream.Id)
	}
	webclient.Client.Send(MakeUserInfo(webclient))
}

func (webclient *WebClient) StreamList(msg *web.StreamListMessage) {
	streams := make([]*web.UserDetailsStreams, 0)
	for _, s := range webclient.Coordinator.Streams {
		if s.Closed {
			continue
		}
		stream := db.GetStreamByOwnerNameAndName(s.Owner, s.Name)
		owner := db.GetUserById(stream.Owner)
		streams = append(streams, MakeStreamInfo(owner, stream, s))
	}
	webclient.Client.Send(&web.StreamListMessage{Streams: streams})
}

func (webclient *WebClient) StreamInfoReq(msg *web.StreamInfoReqMessage) {
	if webclient.Stream != nil {
		for i, c := range webclient.Stream.WebClients {
			if c == webclient {
				webclient.Stream.WebClients = append(webclient.Stream.WebClients[:i], webclient.Stream.WebClients[i+1:]...)
			}
		}
		webclient.Stream = nil
	}

	stream := db.GetStreamByOwnerNameAndName(msg.Owner, msg.Stream)
	if stream == nil {
		if msg.Owner != "" || msg.Stream != "" {
			webclient.Client.Send(&web.InfoErrorMessage{"No such stream"})
		}
		return
	}
	owner := db.GetUserById(stream.Owner)

	running := webclient.Coordinator.Streams[MakeStreamName(owner.Username, stream.Name)]

	if running == nil {
		running = &Stream{
			Owner:       owner.Username,
			Name:        stream.Name,
			Coordinator: webclient.Coordinator,
			Closed:      true,
		}
		webclient.Coordinator.Streams[MakeStreamName(owner.Username, stream.Name)] = running
	}

	webclient.Stream = running
	webclient.Stream.WebClients = append(webclient.Stream.WebClients, webclient)
	webclient.Client.Send(&web.StreamInfoMessage{Stream: MakeStreamInfo(owner, stream, running)})

	msgs := db.PostHistory(stream.Id, 32, db.GetNewestMessage(stream.Id))
	for _, m := range msgs {
		webpost := &web.MessageAddMessage{
			Id:       m.Id,
			Author:   m.User,
			Gravatar: m.Gravatar,
			Edited:   m.Edited,
			Text:     m.Body,
			Posted:   uint64(m.Created.Unix()),
		}
		webclient.Client.Send(webpost)
	}
}

func BroadcastMessage(running *Stream, mid uint64) {
	post := db.GetPost(mid)
	webpost := &web.MessageAddMessage{
		Id:       post.Id,
		Author:   post.User,
		Gravatar: post.Gravatar,
		Edited:   post.Edited,
		Text:     post.Body,
		Posted:   uint64(post.Created.Unix()),
	}

	for _, c := range running.WebClients {
		c.Client.Send(webpost)
	}
}

func (webclient *WebClient) MessageSend(msg *web.MessageSendMessage) {
	stream := db.GetStreamById(msg.Streamid)
	if stream == nil {
		return
	}
	owner := db.GetUserById(stream.Owner)
	running := webclient.Coordinator.Streams[MakeStreamName(owner.Username, stream.Name)]
	if running == nil {
		return
	}

	if len(msg.Text) == 0 {
		return
	}

	if b, _ := regexp.Match("^[ ]+$", []byte(msg.Text)); b {
		return
	}

	mid := db.PostCreate(db.Message{
		User:     webclient.User.Id,
		Stream:   stream.Id,
		Gravatar: MakeGravatar(webclient),
		Body:     msg.Text,
		Edited:   false,
	})
	fmt.Println(mid)
	BroadcastMessage(running, mid)
}

func (webclient *WebClient) MessageEdit(msg *web.MessageEditMessage) {
	post := db.GetPost(msg.Id)
	if post == nil || post.User != webclient.User.Id {
		return
	}
	db.PostUpdate(msg.Id, msg.Text)
	stream := db.GetStreamById(post.Stream)
	owner := db.GetUserById(stream.Owner)
	running := webclient.Coordinator.Streams[MakeStreamName(owner.Username, stream.Name)]
	if running == nil {
		return
	}
	BroadcastMessage(running, msg.Id)
}

func (webclient *WebClient) MessageDelete(msg *web.MessageDeleteMessage) {
	post := db.GetPost(msg.Id)
	if post == nil || post.User != webclient.User.Id {
		return
	}
	db.PostDelete(msg.Id)
}

func (webclient *WebClient) MessageHistory(msg *web.MessageHistoryMessage) {
	stream := db.GetStreamById(msg.Streamid)
	if stream == nil {
		return
	}

	msgs := db.PostHistory(stream.Id, 32, msg.Before-1)
	for _, m := range msgs {
		webpost := &web.MessageAddMessage{
			Id:       m.Id,
			Author:   m.User,
			Gravatar: m.Gravatar,
			Edited:   m.Edited,
			Text:     m.Body,
			Posted:   uint64(m.Created.Unix()),
		}
		webclient.Client.Send(webpost)
	}
}
