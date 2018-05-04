package rtmp

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"time"

	"./amf"
)

var DisconnectError = errors.New("disconnect error")

const (
	SetChunkSizeMessage              = 1
	AcknowledgementMessage           = 3
	UserControlMessage               = 4
	WindowAcknowledgementSizeMessage = 5
	SetPeerBandwidthMessage          = 6
	AudioMessage                     = 8
	VideoMessage                     = 9
	MetadataMessage                  = 18
	CommandMessage                   = 20
)

type ID net.Addr

type ConnectHandler interface {
	ConnectEvent(client ID)
}

type DisconnectHandler interface {
	DisconnectEvent(client ID)
}

type PublishHandler interface {
	PublishEvent(client ID, path string, stream string) bool
}

type SubscribeHandler interface {
	SubscribeEvent(client ID, path string, stream string) bool
}

type InitHandler interface {
	InitEvent(client ID, data *Metadata, audio []byte, video []byte) bool
}

type AudioHandler interface {
	AudioEvent(client ID, data *TimestampBuf) bool
}

type VideoHandler interface {
	VideoEvent(client ID, data *TimestampBuf) bool
}

type Server struct {
	Config             *Config
	Listener           net.Listener
	ConnectHandlers    []ConnectHandler
	DisconnectHandlers []DisconnectHandler
	PublishHandlers    []PublishHandler
	SubscribeHandlers  []SubscribeHandler
	InitHandlers       []InitHandler
	AudioHandlers      []AudioHandler
	VideoHandlers      []VideoHandler
}

type Message interface {
	Type() uint8
	Chunk() uint16
	Stream() uint32
	Render() []byte
	Timestamp() uint32
}

type TimestampBuf struct {
	Data      []byte
	Timestamp uint32
}

type Client struct {
	Id           ID
	Inchunk      []byte
	Outchunk     []byte
	Assembler    map[uint16]*Chunked
	Disassembler map[uint16]*Chunked
	Acklimit     uint32
	Unacked      uint32
	Outbound     chan Message
	Initialized  bool
	AudioInit    []byte
	VideoInit    []byte
	Metadata     *Metadata
	AudioBuf     []*TimestampBuf
	VideoBuf     []*TimestampBuf
	Pathname     string
	LastSeen     time.Time
}

type Chunked struct {
	Chunk     uint16
	Timestamp uint32
	Length    uint32
	Type      uint8
	Stream    uint32
	Delta     uint32
	Extended  bool
	Data      []byte
}

type Metadata struct {
	FrameRate uint32
	AudioRate uint32
	Height    uint32
	Width     uint32
}

func NewServer(custom *Config) *Server {
	config := MergeConfig(defaultConfig, custom)
	if listener, err := net.Listen("tcp", config.Address); err != nil {
		log.Fatal(err)
		return nil
	} else {
		return &Server{
			Config:   config,
			Listener: listener,
		}
	}
}

func (server *Server) Subscribe(subscriber interface{}) {
	if sub, ok := subscriber.(ConnectHandler); ok {
		server.ConnectHandlers = append(server.ConnectHandlers, sub)
	}

	if sub, ok := subscriber.(DisconnectHandler); ok {
		server.DisconnectHandlers = append(server.DisconnectHandlers, sub)
	}

	if sub, ok := subscriber.(PublishHandler); ok {
		server.PublishHandlers = append(server.PublishHandlers, sub)
	}

	if sub, ok := subscriber.(SubscribeHandler); ok {
		server.SubscribeHandlers = append(server.SubscribeHandlers, sub)
	}

	if sub, ok := subscriber.(InitHandler); ok {
		server.InitHandlers = append(server.InitHandlers, sub)
	}

	if sub, ok := subscriber.(AudioHandler); ok {
		server.AudioHandlers = append(server.AudioHandlers, sub)
	}

	if sub, ok := subscriber.(VideoHandler); ok {
		server.VideoHandlers = append(server.VideoHandlers, sub)
	}
}

func (server *Server) SendInitTo(client ID, data *Metadata, audio []byte, video []byte) {

}

func (server *Server) SendAudioTo(client ID, data []byte, timestamp uint32) {

}

func (server *Server) SendVideoTo(client ID, data []byte, timestamp uint32) {

}

func ReadFormat(conn net.Conn) (byte, uint16, error) {
	fst := make([]byte, 1)
	if _, err := io.ReadFull(conn, fst); err != nil {
		return 0, 0, err
	}

	format := fst[0] >> 6
	id := uint16(fst[0]) & 0x3f

	if id == 0 {
		cid := make([]byte, 1)
		if _, err := io.ReadFull(conn, cid); err != nil {
			return 0, 0, err
		}
		id = 64 + uint16(cid[0])
	} else if id == 1 {
		cid := make([]byte, 2)
		if _, err := io.ReadFull(conn, cid); err != nil {
			return 0, 0, err
		}
		id = 64 + uint16(cid[0])*256 + uint16(cid[1])
	}
	return format, id, nil
}

func ReadExtended(conn net.Conn) (uint32, error) {
	buf := make([]byte, 4)
	if _, err := io.ReadFull(conn, buf); err != nil {
		return 0, err
	}
	return ReadB32(buf), nil
}

func ReadHeader(conn net.Conn, format byte, message *Chunked) error {
	switch format {
	case 0:
		buf := make([]byte, 11)
		if _, err := io.ReadFull(conn, buf); err != nil {
			return err
		}
		message.Timestamp = ReadB24(buf[0:3])
		message.Length = ReadB24(buf[3:6])
		message.Type = buf[6]
		message.Stream = ReadL32(buf[7:])
		message.Delta = 0
		message.Extended = false
		if message.Timestamp == 0xFFFFFF {
			if time, err := ReadExtended(conn); err != nil {
				return err
			} else {
				message.Timestamp = time
			}
			message.Extended = true
		}
	case 1:
		buf := make([]byte, 7)
		if _, err := io.ReadFull(conn, buf); err != nil {
			return err
		}
		message.Delta = ReadB24(buf[0:3])
		message.Length = ReadB24(buf[3:6])
		message.Type = buf[6]
		if len(message.Data) == 0 {
			message.Extended = false
			if message.Delta == 0xFFFFFF {
				if time, err := ReadExtended(conn); err != nil {
					return err
				} else {
					message.Delta = time
				}
				message.Extended = true
			}
			message.Timestamp += message.Delta
		}
	case 2:
		buf := make([]byte, 3)
		if _, err := io.ReadFull(conn, buf); err != nil {
			return err
		}
		message.Delta = ReadB24(buf[0:3])
		if len(message.Data) == 0 {
			message.Extended = false
			if message.Delta == 0xFFFFFF {
				if time, err := ReadExtended(conn); err != nil {
					return err
				} else {
					message.Delta = time
				}
				message.Extended = true
			}
			message.Timestamp += message.Delta
		}
	case 3:
		if len(message.Data) == 0 {
			if message.Extended {
				if time, err := ReadExtended(conn); err != nil {
					return err
				} else {
					message.Delta = time
				}
			}
			message.Timestamp += message.Delta
		}
	}
	return nil
}

func ReadBody(conn net.Conn, message *Chunked, inchunk []byte) (uint32, error) {
	length := message.Length - uint32(len(message.Data))
	if length > uint32(len(inchunk)) {
		length = uint32(len(inchunk))
	}

	if _, err := io.ReadFull(conn, inchunk[:length]); err != nil {
		return 0, err
	} else {
		message.Data = append(message.Data, inchunk[:length]...)
	}

	return length, nil
}

func (server *Server) ProcessAmf(client *Client, values []amf.Value) error {
	if len(values) == 0 || values[0].Type() != amf.String {
		return nil
	}

	switch values[0].(*amf.StringValue).Data {
	case "connect":
		client.Pathname = values[2].(*amf.ObjectValue).Data["app"].(*amf.StringValue).Data
		client.Outbound <- &AckSize{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			AckSize: server.Config.Acksize,
		}
		client.Outbound <- &SetPeerBandwidth{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			PeerBandwidth: server.Config.Bandwidth,
		}
		client.Outbound <- &StreamBeginEvent{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			StreamNo: 0,
		}
		client.Outbound <- &Command{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			Values: []amf.Value{
				&amf.StringValue{Data: "_result"},
				values[1].(*amf.NumberValue),
				&amf.ObjectValue{Data: map[string]amf.Value{
					"fmsVer":       &amf.StringValue{Data: "FMS/3,0,1,123"},
					"capabilities": &amf.NumberValue{Data: 31},
				}},
				&amf.ObjectValue{Data: map[string]amf.Value{
					"level":          &amf.StringValue{Data: "status"},
					"code":           &amf.StringValue{Data: "NetConnection.Connect.Success"},
					"description":    &amf.StringValue{Data: "Connection succeeded."},
					"objectEncoding": &amf.NumberValue{Data: 0},
				}},
			},
		}
	case "createStream":
		client.Outbound <- &Command{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			Values: []amf.Value{
				&amf.StringValue{Data: "_result"},
				values[1].(*amf.NumberValue),
				&amf.NullValue{},
				&amf.NumberValue{Data: 1},
			},
		}
	case "play":
		streamName := values[3].(*amf.StringValue).Data
		for _, h := range server.SubscribeHandlers {
			if !h.SubscribeEvent(client.Id, client.Pathname, streamName) {
				return DisconnectError
			}
		}
	case "publish":
		streamName := values[3].(*amf.StringValue).Data
		for _, h := range server.PublishHandlers {
			if !h.PublishEvent(client.Id, client.Pathname, streamName) {
				return DisconnectError
			}
		}
		client.Outbound <- &Command{
			BaseMessage: BaseMessage{
				ChunkId:  3,
				StreamNo: 0,
				Time:     0,
			},
			Values: []amf.Value{
				&amf.StringValue{Data: "onStatus"},
				values[1].(*amf.NumberValue),
				&amf.NullValue{},
				&amf.ObjectValue{Data: map[string]amf.Value{
					"level":          &amf.StringValue{Data: "status"},
					"code":           &amf.StringValue{Data: "NetStream.Publish.Start"},
					"description":    &amf.StringValue{Data: "Start publishing."},
					"objectEncoding": &amf.NumberValue{Data: 0},
				}},
			},
		}
	}
	return nil
}

func (server *Server) ProcessMessage(message *Chunked, client *Client) error {
	wasinit := client.Initialized
	switch message.Type {
	case CommandMessage:
		if err := server.ProcessAmf(client, amf.ReadAll(bytes.NewReader(message.Data))); err != nil {
			return err
		}
	case WindowAcknowledgementSizeMessage:
		client.Acklimit = ReadB32(message.Data)
	case AudioMessage:
		copydata := make([]byte, len(message.Data))
		copy(copydata, message.Data)
		if client.Initialized {
			buf := &TimestampBuf{Data: copydata, Timestamp: message.Timestamp}
			for _, h := range server.AudioHandlers {
				if !h.AudioEvent(client.Id, buf) {
					return DisconnectError
				}
			}
		} else {
			if client.AudioInit == nil {
				client.AudioInit = copydata
				if client.VideoInit != nil && client.Metadata != nil {
					client.Initialized = true
				}
			} else {
				client.AudioBuf = append(client.AudioBuf, &TimestampBuf{Data: copydata, Timestamp: message.Timestamp})
			}
		}
	case VideoMessage:
		copydata := make([]byte, len(message.Data))
		copy(copydata, message.Data)
		if client.Initialized {
			buf := &TimestampBuf{Data: copydata, Timestamp: message.Timestamp}
			for _, h := range server.VideoHandlers {
				if !h.VideoEvent(client.Id, buf) {
					return DisconnectError
				}
			}
		} else {
			if client.VideoInit == nil {
				client.VideoInit = copydata
				if client.AudioInit != nil && client.Metadata != nil {
					client.Initialized = true
				}
			} else {
				client.VideoBuf = append(client.VideoBuf, &TimestampBuf{Data: copydata, Timestamp: message.Timestamp})
			}
		}
	case MetadataMessage:
		if !client.Initialized {
			values := amf.ReadAll(bytes.NewReader(message.Data))
			client.Metadata = &Metadata{}
			client.Metadata.Height = uint32(values[2].(*amf.ArrayValue).Data["height"].(*amf.NumberValue).Data)
			client.Metadata.Width = uint32(values[2].(*amf.ArrayValue).Data["width"].(*amf.NumberValue).Data)
			client.Metadata.FrameRate = uint32(values[2].(*amf.ArrayValue).Data["framerate"].(*amf.NumberValue).Data)
			client.Metadata.AudioRate = uint32(values[2].(*amf.ArrayValue).Data["audiosamplerate"].(*amf.NumberValue).Data)

			if client.AudioInit != nil && client.VideoInit != nil {
				client.Initialized = true
			}
		}
	case SetChunkSizeMessage:
		client.Inchunk = make([]byte, ReadB32(message.Data))
	default:
	}

	if !wasinit && client.Initialized {
		for _, h := range server.InitHandlers {
			if !h.InitEvent(client.Id, client.Metadata, client.AudioInit, client.VideoInit) {
				return DisconnectError
			}
		}

		for _, buf := range client.AudioBuf {
			for _, h := range server.AudioHandlers {
				if !h.AudioEvent(client.Id, buf) {
					return DisconnectError
				}
			}
		}

		for _, buf := range client.VideoBuf {
			for _, h := range server.VideoHandlers {
				if !h.VideoEvent(client.Id, buf) {
					return DisconnectError
				}
			}
		}
	}
	client.LastSeen = time.Now()
	return nil
}

func (server *Server) Converse(conn net.Conn, client *Client, context *Server) {
	for {
		format, id, err := ReadFormat(conn)
		if err != nil {
			return
		}

		if _, ok := client.Assembler[id]; !ok {
			if format != 0 {
				return
			}
			client.Assembler[id] = &Chunked{
				Chunk:  id,
				Stream: 1,
			}
		}
		message := client.Assembler[id]
		if err := ReadHeader(conn, format, message); err != nil {
			return
		}
		if n, err := ReadBody(conn, message, client.Inchunk); err != nil {
			return
		} else {
			client.Unacked += n
		}
		if client.Acklimit > 0 && client.Unacked >= client.Acklimit {
			client.Outbound <- &Ack{
				BaseMessage: BaseMessage{
					ChunkId:  3,
					StreamNo: 0,
					Time:     0,
				},
				Acked: client.Unacked,
			}
			client.Unacked = 0
		}
		if uint32(len(message.Data)) >= message.Length {
			if err := server.ProcessMessage(message, client); err != nil {
				return
			}
			message.Data = message.Data[:0]
		}
	}
}

func MakeChunkBuf(chunk uint16, format int) []byte {
	if chunk > 319 {
		buf := make([]byte, 3)
		buf[0] = byte(1 | (format << 6))
		WriteB16(buf[1:], chunk-64)
		return buf
	} else if chunk > 64 {
		buf := make([]byte, 2)
		buf[0] = byte(1 | (format << 6))
		buf[1] = byte(chunk - 64)
		return buf
	} else {
		buf := make([]byte, 1)
		buf[0] = byte(chunk) | byte(format<<6)
		return buf
	}
}

func (server *Server) Send(conn net.Conn, client *Client) {
	for message := range client.Outbound {
		data := message.Render()
		format := 0
		delta := uint32(0)
		if old, ok := client.Disassembler[message.Chunk()]; ok {
			delta = message.Timestamp() - old.Timestamp
			if old.Stream == message.Stream() && message.Stream() != 0 {
				format = 1
				if old.Type == message.Type() && old.Length == uint32(len(data)) {
					format = 2
					if old.Timestamp+old.Delta == message.Timestamp() {
						format = 3
					}
				}
			}
		}
		client.Disassembler[message.Chunk()] = &Chunked{
			Chunk:     message.Chunk(),
			Delta:     delta,
			Timestamp: message.Timestamp(),
			Length:    uint32(len(data)),
			Type:      message.Type(),
			Stream:    message.Stream(),
		}

		if _, err := conn.Write(MakeChunkBuf(message.Chunk(), format)); err != nil {
			return
		}

		switch format {
		case 0:
			buf := make([]byte, 11)
			WriteB24(buf[0:3], message.Timestamp())
			WriteB24(buf[3:6], uint32(len(data)))
			buf[6] = message.Type()
			WriteL32(buf[7:], message.Stream())
			if _, err := conn.Write(buf); err != nil {
				return
			}
		case 1:
			buf := make([]byte, 7)
			WriteB24(buf[0:3], delta)
			WriteB24(buf[3:6], uint32(len(data)))
			buf[6] = message.Type()
			if _, err := conn.Write(buf); err != nil {
				return
			}
		case 2:
			buf := make([]byte, 3)
			WriteB24(buf[0:3], delta)
			if _, err := conn.Write(buf); err != nil {
				return
			}
		case 3:
		}

		csiz := len(client.Outchunk)

		for len(data) > 0 {
			m := len(data)
			if m > csiz {
				m = csiz
			}
			buf := data[:m]
			if _, err := conn.Write(buf); err != nil {
				return
			}
			if len(data) > csiz {
				data = data[csiz:]
				if _, err := conn.Write(MakeChunkBuf(message.Chunk(), 3)); err != nil {
					return
				}
			} else {
				break
			}
		}

		if msg, ok := message.(*SetChunkSize); ok {
			client.Outchunk = make([]byte, msg.ChunkSize)
		}
	}
}

func Watchdog(conn net.Conn, client *Client) {
	for {
		time.Sleep(20 * time.Second)
		secs := time.Now().Sub(client.LastSeen).Seconds()
		if secs > 30 {
			fmt.Println(secs)
			conn.Close()
			break
		}
	}
}

func (server *Server) Serve(conn net.Conn, id ID) {
	defer conn.Close()
	if err := Handshake(conn); err != nil {
		return
	}
	client := &Client{
		Id:           id,
		Inchunk:      make([]byte, 128),
		Outchunk:     make([]byte, 128),
		Assembler:    make(map[uint16]*Chunked),
		Disassembler: make(map[uint16]*Chunked),
		Acklimit:     0,
		Unacked:      0,
		Outbound:     make(chan Message),
		LastSeen:     time.Now(),
	}

	go server.Send(conn, client)

	for _, h := range server.ConnectHandlers {
		h.ConnectEvent(id)
	}
	go Watchdog(conn, client)
	server.Converse(conn, client, server)
	close(client.Outbound)
	for _, h := range server.DisconnectHandlers {
		h.DisconnectEvent(id)
	}
}

func (server *Server) ListenAndServe() error {
	for {
		if conn, err := server.Listener.Accept(); err != nil {
			return err
		} else {
			go server.Serve(conn, conn.RemoteAddr())
		}
	}
}
