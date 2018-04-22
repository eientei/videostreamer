package rtmp

import (
	"bytes"
	"errors"
	"io"
	"log"
	"net"
	"os"

	"../amf"
	"../util"

	"../server"
)

var InvalidAmf = errors.New("invalid amf")
var ClosingConnection = errors.New("closing connection")

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

const (
	StreamBeginUserEvent = 0
)

type Message struct {
	Chunk     uint16
	Timestamp uint32
	Length    uint32
	Type      uint8
	Stream    uint32
	Delta     uint32
	Data      []byte
	Extended  bool
}

type Client struct {
	Context  *server.Context
	Conn     net.Conn
	Drainer  []byte
	InChunk  []byte
	OutChunk []byte
	Assembly map[uint16]*Message
	Acksize  uint32
	Logger   *log.Logger
	Unacked  uint32
	Stream   *server.Stream
}

func (client *Client) SendMessage(message *Message) error {
	makeChunkBuf := func(message *Message, fst int) []byte {
		if message.Chunk > 319 {
			buf := make([]byte, 3)
			buf[0] = byte(1 | (fst << 6))
			util.WriteB16(buf[1:], message.Chunk-64)
			return buf
		} else if message.Chunk > 64 {
			buf := make([]byte, 2)
			buf[0] = byte(1 | (fst << 6))
			buf[1] = byte(message.Chunk - 64)
			return buf
		} else {
			buf := make([]byte, 1)
			buf[0] = byte(message.Chunk) | byte(fst<<6)
			return buf
		}
	}

	fst := 0
	if _, err := client.Conn.Write(makeChunkBuf(message, fst)); err != nil {
		return err
	}
	switch fst {
	case 0:
		buf := make([]byte, 11)
		util.WriteB24(buf[0:3], message.Timestamp)
		util.WriteB24(buf[3:6], message.Length)
		buf[6] = message.Type
		util.WriteL32(buf[7:], message.Stream)
		if _, err := client.Conn.Write(buf); err != nil {
			return err
		}
	case 1:
		buf := make([]byte, 7)
		util.WriteB24(buf[0:3], message.Delta)
		util.WriteB24(buf[3:6], message.Length)
		buf[6] = message.Type
		if _, err := client.Conn.Write(buf); err != nil {
			return err
		}
	case 2:
		buf := make([]byte, 3)
		util.WriteB24(buf[0:3], message.Delta)
		if _, err := client.Conn.Write(buf); err != nil {
			return err
		}
	case 3:
	}

	data := message.Data
	csiz := len(client.OutChunk)
	for len(data) > 0 {
		m := len(data)
		if m > csiz {
			m = csiz
		}
		buf := data[:m]
		if _, err := client.Conn.Write(buf); err != nil {
			return err
		}
		if len(data) > csiz {
			data = data[csiz:]
			if _, err := client.Conn.Write(makeChunkBuf(message, 3)); err != nil {
				return err
			}
		} else {
			break
		}
	}
	return nil
}

func (client *Client) SendWinAckSize(ackSize uint32) error {
	ackbuf := make([]byte, 4)
	util.WriteB32(ackbuf, ackSize)
	return client.SendMessage(&Message{
		Chunk:     0x03,
		Timestamp: 0,
		Length:    4,
		Type:      WindowAcknowledgementSizeMessage,
		Stream:    0x00,
		Delta:     0,
		Data:      ackbuf,
	})
}

func (client *Client) SendSetPeerBandwidth(bandwidth uint32) error {
	bandbuf := make([]byte, 5)
	util.WriteB32(bandbuf, bandwidth)
	bandbuf[4] = 0x01
	return client.SendMessage(&Message{
		Chunk:     0x03,
		Timestamp: 0,
		Length:    5,
		Type:      SetPeerBandwidthMessage,
		Stream:    0x00,
		Delta:     0,
		Data:      bandbuf,
	})
}

func (client *Client) SendStreamBeginEvent(stream uint32) error {
	streambuf := make([]byte, 6)
	util.WriteB16(streambuf, StreamBeginUserEvent)
	util.WriteL32(streambuf[2:], stream)
	return client.SendMessage(&Message{
		Chunk:     0x03,
		Timestamp: 0,
		Length:    6,
		Type:      UserControlMessage,
		Stream:    0,
		Delta:     0,
		Data:      streambuf,
	})
}

func (client *Client) SendCommand(values []amf.Value) error {
	buf := &bytes.Buffer{}
	amf.WriteAll(buf, values)
	return client.SendMessage(&Message{
		Chunk:     0x03,
		Timestamp: 0,
		Length:    uint32(buf.Len()),
		Type:      CommandMessage,
		Stream:    0,
		Delta:     0,
		Data:      buf.Bytes(),
	})
}

func (client *Client) SendAck(ack uint32) error {
	ackbuf := make([]byte, 4)
	util.WriteB32(ackbuf, client.Unacked)
	return client.SendMessage(&Message{
		Chunk:     0x03,
		Timestamp: 0,
		Length:    4,
		Type:      AcknowledgementMessage,
		Stream:    0,
		Delta:     0,
		Data:      ackbuf,
	})
}

func (client *Client) ProcessAmf(values []amf.Value) error {
	if len(values) == 0 || values[0].Type() != amf.String {
		return nil
	}
	switch values[0].(*amf.StringValue).Data {
	case "connect":
		if err := client.SendWinAckSize(0xFFFFFF); err != nil {
			return err
		}
		if err := client.SendSetPeerBandwidth(0xFFFFFF); err != nil {
			return err
		}
		if err := client.SendStreamBeginEvent(0); err != nil {
			return err
		}
		if err := client.SendCommand([]amf.Value{
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
		}); err != nil {
			return err
		}
	case "createStream":
		if err := client.SendCommand([]amf.Value{
			&amf.StringValue{Data: "_result"},
			values[1].(*amf.NumberValue),
			&amf.NullValue{},
			&amf.NumberValue{Data: 1},
		}); err != nil {
			return err
		}
	case "play":
		if err := client.SendCommand([]amf.Value{
			&amf.StringValue{Data: "onStatus"},
			values[1].(*amf.NumberValue),
			&amf.NullValue{},
			&amf.ObjectValue{Data: map[string]amf.Value{
				"level":          &amf.StringValue{Data: "error"},
				"code":           &amf.StringValue{Data: "NetStream.Play.StreamNotFound"},
				"description":    &amf.StringValue{Data: "Playing not implemented (yet)."},
				"objectEncoding": &amf.NumberValue{Data: 0},
			}},
		}); err != nil {
			return err
		}
		return ClosingConnection
	case "publish":
		streamName := values[3].(*amf.StringValue).Data
		if _, ok := client.Context.Streams[streamName]; ok {
			if err := client.SendCommand([]amf.Value{
				&amf.StringValue{Data: "onStatus"},
				values[1].(*amf.NumberValue),
				&amf.NullValue{},
				&amf.ObjectValue{Data: map[string]amf.Value{
					"level":          &amf.StringValue{Data: "error"},
					"code":           &amf.StringValue{Data: "NetStream.Publish.AlreadyPublishing"},
					"description":    &amf.StringValue{Data: "Stream already publishing."},
					"objectEncoding": &amf.NumberValue{Data: 0},
				}},
			}); err != nil {
				return err
			}
			return ClosingConnection
		}
		if err := client.SendStreamBeginEvent(1); err != nil {
			return err
		}
		if err := client.SendCommand([]amf.Value{
			&amf.StringValue{Data: "onStatus"},
			values[1].(*amf.NumberValue),
			&amf.NullValue{},
			&amf.ObjectValue{Data: map[string]amf.Value{
				"level":          &amf.StringValue{Data: "status"},
				"code":           &amf.StringValue{Data: "NetStream.Publish.Start"},
				"description":    &amf.StringValue{Data: "Start publishing."},
				"objectEncoding": &amf.NumberValue{Data: 0},
			}},
		}); err != nil {
			return err
		}

		client.Logger.Println("Open stream [/" + streamName + "]")
		client.Stream = &server.Stream{
			AudioIn: make(chan []byte, 64),
			VideoIn: make(chan []byte, 64),
			Name:   streamName,
			Logger: log.New(os.Stdout, "[/"+streamName+"] ", log.LstdFlags),
		}
		client.Context.Streams[streamName] = client.Stream
		go client.Stream.Run()_
	}
	return nil
}

func (client *Client) ProcessMessage(message *Message) error {
	//client.Logger.Println(message.Chunk, message.Timestamp, message.Length, message.Type, message.Stream, message.Delta)
	switch message.Type {
	case CommandMessage:
		if err := client.ProcessAmf(amf.ReadAll(bytes.NewReader(message.Data))); err != nil {
			return err
		}
	case WindowAcknowledgementSizeMessage:
		client.Acksize = util.ReadB32(message.Data)
	case AudioMessage:
		copydata := make([]byte, len(message.Data))
		copy(copydata, message.Data)
		client.Stream.AudioIn <- copydata
	case VideoMessage:
		copydata := make([]byte, len(message.Data))
		copy(copydata, message.Data)
		client.Stream.VideoIn <- copydata
	case MetadataMessage:
		if err := client.Stream.Meta(message.Data); err != nil {
			return err
		}
	case SetChunkSizeMessage:
		client.InChunk = make([]byte, util.ReadB32(message.Data))
		client.Drainer = make([]byte, len(client.InChunk)*10)
	default:
		//if message.Type == 0 {
		//if _, err := io.ReadFull(client.Conn, client.Drainer); err != nil {
		//return err
		//}
		//}
		//client.Logger.Println(message)
	}
	return nil
}

func (client *Client) ReadFormat() (uint8, uint16, error) {
	fst := make([]byte, 1)
	if _, err := io.ReadFull(client.Conn, fst); err != nil {
		return 0, 0, err
	}
	client.Unacked += 1

	format := fst[0] >> 6
	id := uint16(fst[0]) & 0x3f

	if id == 0 {
		cid := make([]byte, 1)
		if _, err := io.ReadFull(client.Conn, cid); err != nil {
			return 0, 0, err
		}
		client.Unacked += 1
		id = 64 + uint16(cid[0])
	} else if id == 1 {
		cid := make([]byte, 2)
		if _, err := io.ReadFull(client.Conn, cid); err != nil {
			return 0, 0, err
		}
		client.Unacked += 2
		id = 64 + uint16(cid[0])*256 + uint16(cid[1])
	}
	return format, id, nil
}

func (client *Client) Converse() error {
	format, id, err := client.ReadFormat()
	if err != nil {
		return err
	}

	if _, ok := client.Assembly[id]; !ok {
		client.Assembly[id] = &Message{
			Chunk:  id,
			Stream: 1,
		}
		for {
			if format == 0 || format == 1 {
				break
			}
			client.Logger.Println("SKIP", format)
			if _, err := io.ReadFull(client.Conn, client.Drainer); err != nil {
				return err
			}
			format, id, err = client.ReadFormat()
			if err != nil {
				return err
			}
		}
	}

	message := client.Assembly[id]

	switch format {
	case 0:
		buf := make([]byte, 11)
		if _, err := io.ReadFull(client.Conn, buf); err != nil {
			return err
		}
		client.Unacked += 11
		message.Timestamp = util.ReadB24(buf[0:3])
		message.Length = util.ReadB24(buf[3:6])
		message.Type = buf[6]
		message.Stream = util.ReadL32(buf[7:])
		message.Delta = 0
		message.Extended = false
		if message.Timestamp == 0xFFFFFF {
			buf := make([]byte, 4)
			if _, err := io.ReadFull(client.Conn, buf); err != nil {
				return err
			}
			client.Unacked += 4
			message.Timestamp = util.ReadB32(buf)
			message.Extended = true
		}
	case 1:
		buf := make([]byte, 7)
		if _, err := io.ReadFull(client.Conn, buf); err != nil {
			return err
		}
		client.Unacked += 7
		message.Delta = util.ReadB24(buf[0:3])
		message.Length = util.ReadB24(buf[3:6])
		message.Type = buf[6]
		if len(message.Data) == 0 {
			message.Extended = false
			if message.Delta == 0xFFFFFF {
				buf := make([]byte, 4)
				if _, err := io.ReadFull(client.Conn, buf); err != nil {
					return err
				}
				client.Unacked += 4
				message.Delta = util.ReadB32(buf)
				message.Extended = true
			}
			message.Timestamp += message.Delta
		}
	case 2:
		buf := make([]byte, 3)
		if _, err := io.ReadFull(client.Conn, buf); err != nil {
			return err
		}
		client.Unacked += 3
		message.Delta = util.ReadB24(buf[0:3])
		if len(message.Data) == 0 {
			message.Extended = false
			if message.Delta == 0xFFFFFF {
				buf := make([]byte, 4)
				if _, err := io.ReadFull(client.Conn, buf); err != nil {
					return err
				}
				client.Unacked += 4
				message.Delta = util.ReadB32(buf)
				message.Extended = true
			}
			message.Timestamp += message.Delta
		}
	case 3:
		if len(message.Data) == 0 {
			if message.Extended {
				buf := make([]byte, 4)
				if _, err := io.ReadFull(client.Conn, buf); err != nil {
					return err
				}
				client.Unacked += 4
				message.Delta = util.ReadB32(buf)
			}
			message.Timestamp += message.Delta
		}
	}

	//if message.Extended {
	//fmt.Println("EXTEND")
	//}

	length := message.Length - uint32(len(message.Data))
	if length > uint32(len(client.InChunk)) {
		length = uint32(len(client.InChunk))
	}

	if _, err := io.ReadFull(client.Conn, client.InChunk[:length]); err != nil {
		return err
	} else {
		client.Unacked += uint32(length)
		message.Data = append(message.Data, client.InChunk[:length]...)
	}

	if message.Length <= uint32(len(message.Data)) {
		if err := client.ProcessMessage(message); err != nil {
			return err
		}
		message.Data = message.Data[:0]
	}

	if client.Acksize > 0 && client.Acksize <= client.Unacked {
		if err := client.SendAck(client.Unacked); err != nil {
			return err
		}
		client.Unacked = 0
	}
	return nil
}

func Serve(context *server.Context, conn net.Conn, logger *log.Logger) error {
	if err := Handshake(conn); err != nil {
		return err
	}

	client := &Client{
		Context:  context,
		Conn:     conn,
		Drainer:  make([]byte, 128*10),
		InChunk:  make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*Message),
		Acksize:  0,
		Logger:   logger,
	}

	defer func() {
		if client.Stream != nil {
			client.Stream.Close()
			delete(client.Context.Streams, client.Stream.Name)
		}
	}()

	for {
		if err := client.Converse(); err != nil {
			return err
		}
	}
}

func Server(listen string, context *server.Context) error {
	if listener, err := net.Listen("tcp", listen); err != nil {
		return err
	} else {
		for {
			if conn, err := listener.Accept(); err != nil {
				listener.Close()
				return err
			} else {
				go func() {
					logger := log.New(os.Stdout, "RTMP client "+conn.RemoteAddr().String()+" ", log.LstdFlags)
					logger.Println("Connected")
					if err := Serve(context, conn, logger); err != nil {
						logger.Println("Disconnected with", err)
					}
					conn.Close()
				}()
			}
		}
		return listener.Close()
	}
}
