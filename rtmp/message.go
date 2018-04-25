package rtmp

import (
	"bytes"

	"./amf"
)

type BaseMessage struct {
	ChunkId  uint16
	StreamNo uint32
	Time     uint32
}

func (message *BaseMessage) Chunk() uint16 {
	return message.ChunkId
}

func (message *BaseMessage) Stream() uint32 {
	return message.StreamNo
}

func (message *BaseMessage) Timestamp() uint32 {
	return message.Time
}

type SetChunkSize struct {
	BaseMessage
	ChunkSize uint32
}

func (message *SetChunkSize) Type() uint8 {
	return SetChunkSizeMessage
}

func (message *SetChunkSize) Render() []byte {
	chunkbuf := make([]byte, 4)
	WriteB32(chunkbuf, message.ChunkSize)
	return chunkbuf
}

type AckSize struct {
	BaseMessage
	AckSize uint32
}

func (message *AckSize) Type() uint8 {
	return WindowAcknowledgementSizeMessage
}

func (message *AckSize) Render() []byte {
	ackbuf := make([]byte, 4)
	WriteB32(ackbuf, message.AckSize)
	return ackbuf
}

type SetPeerBandwidth struct {
	BaseMessage
	PeerBandwidth uint32
}

func (message *SetPeerBandwidth) Type() uint8 {
	return SetPeerBandwidthMessage
}

func (message *SetPeerBandwidth) Render() []byte {
	bandbuf := make([]byte, 5)
	WriteB32(bandbuf, message.PeerBandwidth)
	bandbuf[4] = 0x01
	return bandbuf
}

type StreamBeginEvent struct {
	BaseMessage
	StreamNo uint32
}

func (message *StreamBeginEvent) Type() uint8 {
	return UserControlMessage
}

func (message *StreamBeginEvent) Render() []byte {
	streambuf := make([]byte, 6)
	WriteB16(streambuf, 0)
	WriteL32(streambuf[2:], message.StreamNo)
	return streambuf
}

type Command struct {
	BaseMessage
	Values []amf.Value
}

func (message *Command) Type() uint8 {
	return CommandMessage
}

func (message *Command) Render() []byte {
	buf := &bytes.Buffer{}
	amf.WriteAll(buf, message.Values)
	return buf.Bytes()
}

type Ack struct {
	BaseMessage
	Acked uint32
}

func (message *Ack) Type() uint8 {
	return AcknowledgementMessage
}

func (message *Ack) Render() []byte {
	ackbuf := make([]byte, 4)
	WriteB32(ackbuf, message.Acked)
	return ackbuf
}
