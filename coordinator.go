package main

import (
	"log"
	"os"

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
		var data []byte
		var atime uint32
		var vtime uint32
		if c.Sequence() == 0 {
			first := 0
			for i, c := range event.VideoBuffer {
				if c.SliceType == 7 {
					first = i
					break
				}
			}
			data, atime, vtime = stream.Muxer.RenderEvent(&mp4.MuxEvent{AudioBuffer: event.AudioBuffer, VideoBuffer: event.VideoBuffer[first:]}, c.Sequence(), c.Atime(), c.Vtime())
		} else {
			data, atime, vtime = stream.Muxer.RenderEvent(event, c.Sequence(), c.Atime(), c.Vtime())
		}
		c.Send(data)
		c.Advance(1, uint64(atime), uint64(vtime))
		/*
			abuf := make([]byte, 8)
			vbuf := make([]byte, 8)
			sbuf := make([]byte, 4)

			WriteB64(abuf, c.Atime())
			WriteB64(vbuf, c.Vtime())
			WriteB32(sbuf, c.Sequence())

			b := make([]byte, 0)
			b = append(b, mux.PrePresent...)
			b = append(b, vbuf...)
			b = append(b, mux.PreSequence...)
			b = append(b, sbuf...)
			b = append(b, mux.PreTrack1...)
			b = append(b, vbuf...)
			b = append(b, mux.PreTrack2...)
			b = append(b, abuf...)
			b = append(b, mux.Trailer...)

			go c.Send(b)

		*/
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
}

func (coordinator *Coordinator) ClientConnect(client web.Client, name string) bool {
	if stream, ok := coordinator.Streams[name]; !ok {
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
		Detail: name,
	}

	return true
}

func (coordinator *Coordinator) ClientDisconnect(client web.Client, name string) {
	if stream, ok := coordinator.Streams[name]; ok {
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
		Detail: name,
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
			Detail: coordinator.RtmpClients[client].Stream.Name,
		}
		close(coordinator.RtmpClients[client].Stream.AudioBuffer)
		close(coordinator.RtmpClients[client].Stream.VideoBuffer)
		delete(coordinator.Streams, coordinator.RtmpClients[client].Stream.Name)
	case SubscriberRole:
		coordinator.Events <- &Event{
			Source: client.String(),
			Type:   UnsubscribedEvent,
			Detail: coordinator.RtmpClients[client].Stream.Name,
		}
	}
	delete(coordinator.RtmpClients, client)
}

func (coordinator *Coordinator) PublishEvent(client rtmp.ID, path string, stream string) bool {
	if _, ok := coordinator.Streams[stream]; ok {
		return false
	} else {
		s := &Stream{
			Path:        path,
			Name:        stream,
			AudioBuffer: make(chan *rtmp.TimestampBuf),
			VideoBuffer: make(chan *rtmp.TimestampBuf),
		}
		coordinator.Streams[stream] = s
		coordinator.RtmpClients[client].Role = PublisherRole
		coordinator.RtmpClients[client].Stream = s

		coordinator.Events <- &Event{
			Source: client.String(),
			Type:   PublishedEvent,
			Detail: stream,
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
		Detail: coordinator.RtmpClients[client].Stream.Name,
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
		}
	}
}
