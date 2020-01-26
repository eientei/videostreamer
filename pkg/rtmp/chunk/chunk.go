package chunk

import (
	"bytes"
	"errors"
	"time"

	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

var (
	// ErrInvalidChunkFormat indicates invalid chunk format
	ErrInvalidChunkFormat = errors.New("invalid chunk format")
)

// Chunk represents state of RTMP chunk stream
type Chunk struct {
	ID        uint32       // chunk stream ID
	TypeID    uint8        // message type ID
	Length    uint32       // message length
	StreamID  uint32       // stream stream ID
	Timestamp uint32       // timestamp (ms) relative to local delta
	Delta     uint32       // delta (ms) relative to last message
	Data      bytes.Buffer // message body
}

// Complete indicates that this chunk stream contains complete message
func (ch *Chunk) Complete() bool {
	return uint32(ch.Data.Len()) == ch.Length
}

// Message returns assembled raw message
func (ch *Chunk) Message(timestamp time.Time, peerDelta uint32) *message.Raw {
	data := make([]byte, ch.Length)
	copy(data, ch.Data.Bytes())

	return &message.Raw{
		Header: message.Header{
			Timestamp: timestamp.Add(time.Duration(ch.Timestamp-peerDelta) * time.Millisecond),
			Delta:     time.Duration(ch.Delta) * time.Millisecond,
			Length:    ch.Length,
			Type:      message.Type(ch.TypeID),
			StreamID:  ch.StreamID,
			ChunkID:   ch.ID,
		},
		Data: data,
	}
}
