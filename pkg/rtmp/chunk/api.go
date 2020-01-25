// Package chunk provides reading and writing messages using RTMP chunk streams
package chunk

import (
	"bytes"
	"errors"
	"io"
	"time"

	"github.com/eientei/videostreamer/internal/byteorder"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

// Header format codes
const (
	HeaderFormatFull   byte = 0x00
	HeaderFormatMedium byte = 0x01
	HeaderFormatShort  byte = 0x02
	HeaderFormatNone   byte = 0x03
)

const (
	chunkBits     = 6
	chunkOffset   = 1 << chunkBits
	chunkMask     = chunkOffset - 1
	chunkLong     = 0x13f
	chunkMediumID = 0
	chunkLongID   = 1
	maxTime       = 0xFFFFFF
)

var (
	// ErrInvalidChunkFormat indicates invalid chunk format
	ErrInvalidChunkFormat = errors.New("invalid chunk format")
)

// NewChunks creates inbound or outbound chunks with given connection epoch timestamp and peer time delta
func NewChunks(timestamp time.Time, peerDelta uint32) *Chunks {
	return &Chunks{
		ChunkMap:     make(map[uint32]*Chunk),
		HeaderBuffer: make([]byte, 18),
		DataBuffer:   make([]byte, 128),
		PeerDelta:    peerDelta,
		Timestamp:    timestamp,
	}
}

// Chunks manages inbound or outbound RTMP chunk streams
type Chunks struct {
	ChunkMap     map[uint32]*Chunk
	HeaderBuffer []byte
	DataBuffer   []byte
	PeerDelta    uint32
	Timestamp    time.Time
}

// SetDataBuffer updates chunk buffer size
func (chs *Chunks) SetDataBuffer(data []byte) {
	chs.DataBuffer = data
}

func (chs *Chunks) readBasicHeader(r io.Reader) (format byte, ch *Chunk, err error) {
	_, err = io.ReadFull(r, chs.HeaderBuffer[:1])
	if err != nil {
		return
	}

	format = chs.HeaderBuffer[0] >> chunkBits
	chunkID := uint32(chs.HeaderBuffer[0]) & chunkMask

	switch chunkID {
	case chunkMediumID:
		_, err = io.ReadFull(r, chs.HeaderBuffer[:1])
		if err != nil {
			return
		}

		chunkID = uint32(chs.HeaderBuffer[0]) + chunkOffset
	case chunkLongID:
		_, err = io.ReadFull(r, chs.HeaderBuffer[:2])
		if err != nil {
			return
		}

		chunkID = uint32(byteorder.BigEndian.Uint16(chs.HeaderBuffer)) + chunkOffset
	}

	if c, ok := chs.ChunkMap[chunkID]; !ok {
		c = &Chunk{
			ID:        chunkID,
			Timestamp: chs.PeerDelta,
		}
		ch, chs.ChunkMap[chunkID] = c, c
	} else {
		ch = c
	}

	return
}

func (chs *Chunks) readHeaderFull(r io.Reader, ch *Chunk) (err error) {
	_, err = io.ReadFull(r, chs.HeaderBuffer[:11])
	if err != nil {
		return
	}

	prevStamp := ch.Timestamp
	ch.Timestamp = byteorder.BigEndian.Uint24(chs.HeaderBuffer[:3])
	ch.Length = byteorder.BigEndian.Uint24(chs.HeaderBuffer[3:6])
	ch.TypeID = chs.HeaderBuffer[6]
	ch.StreamID = byteorder.BigEndian.Uint32(chs.HeaderBuffer[7:11])

	if ch.Timestamp == maxTime {
		_, err = io.ReadFull(r, chs.HeaderBuffer[:4])
		if err != nil {
			return
		}

		ch.Timestamp = byteorder.BigEndian.Uint32(chs.HeaderBuffer[:4])
	}

	ch.Delta = ch.Timestamp - prevStamp

	return
}

func (chs *Chunks) readHeaderMedium(r io.Reader, ch *Chunk) (err error) {
	_, err = io.ReadFull(r, chs.HeaderBuffer[:7])
	if err != nil {
		return
	}

	ch.Delta = byteorder.BigEndian.Uint24(chs.HeaderBuffer[:3])
	ch.Length = byteorder.BigEndian.Uint24(chs.HeaderBuffer[3:6])
	ch.TypeID = chs.HeaderBuffer[6]

	if ch.Delta == maxTime {
		_, err = io.ReadFull(r, chs.HeaderBuffer[:4])
		if err != nil {
			return
		}

		ch.Delta = byteorder.BigEndian.Uint32(chs.HeaderBuffer[:4])
	}

	ch.Timestamp += ch.Delta

	return
}

func (chs *Chunks) readHeaderShort(r io.Reader, ch *Chunk) (err error) {
	_, err = io.ReadFull(r, chs.HeaderBuffer[:3])
	if err != nil {
		return
	}

	ch.Delta = byteorder.BigEndian.Uint24(chs.HeaderBuffer[:3])

	if ch.Delta == maxTime {
		_, err = io.ReadFull(r, chs.HeaderBuffer[:4])
		if err != nil {
			return
		}

		ch.Delta = byteorder.BigEndian.Uint32(chs.HeaderBuffer[:4])
	}

	ch.Timestamp += ch.Delta

	return
}

// ReadChunk reads next chunk (possibly incomplete) from given reader
func (chs *Chunks) ReadChunk(r io.Reader) (ch *Chunk, err error) {
	format, ch, err := chs.readBasicHeader(r)
	if err != nil {
		return
	}

	if ch.Complete() {
		switch format {
		case HeaderFormatFull:
			err = chs.readHeaderFull(r, ch)
		case HeaderFormatMedium:
			err = chs.readHeaderMedium(r, ch)
		case HeaderFormatShort:
			err = chs.readHeaderShort(r, ch)
		case HeaderFormatNone:
			ch.Timestamp += ch.Delta
		default:
			return nil, ErrInvalidChunkFormat
		}

		ch.Data.Reset()

		if err != nil {
			return
		}
	} else {
		switch format {
		case HeaderFormatNone:
		default:
			return nil, ErrInvalidChunkFormat
		}
	}

	expect := ch.Length - uint32(ch.Data.Len())

	if expect > uint32(len(chs.DataBuffer)) {
		expect = uint32(len(chs.DataBuffer))
	}

	_, err = io.ReadFull(r, chs.DataBuffer[:expect])
	if err != nil {
		return
	}

	_, err = ch.Data.Write(chs.DataBuffer[:expect])

	return
}

// ReadMessage repeats reading chunks from given reader until complete message is read
func (chs *Chunks) ReadMessage(r io.Reader) (m *message.Raw, err error) {
	for {
		ch, err := chs.ReadChunk(r)
		if err != nil {
			return nil, err
		}

		if ch.Complete() {
			return ch.Message(chs.Timestamp, chs.PeerDelta), nil
		}
	}
}

func (chs *Chunks) writeHeaderFull(header []byte, timestamp uint32, message *message.Raw) []byte {
	t := timestamp
	if t > maxTime {
		t = maxTime
	}

	byteorder.BigEndian.PutUint24(header[:3], t)
	byteorder.BigEndian.PutUint24(header[3:6], message.Length)
	header[6] = uint8(message.Type)
	byteorder.BigEndian.PutUint32(header[7:11], message.StreamID)

	if t == maxTime {
		byteorder.BigEndian.PutUint32(header[11:15], timestamp)
		return header[:15]
	}

	return header[:11]
}

func (chs *Chunks) writeHeaderMedium(header []byte, delta uint32, message *message.Raw) []byte {
	d := delta
	if d > maxTime {
		d = maxTime
	}

	byteorder.BigEndian.PutUint24(header[:3], d)
	byteorder.BigEndian.PutUint24(header[3:6], message.Length)
	header[6] = uint8(message.Type)

	if d == maxTime {
		byteorder.BigEndian.PutUint32(header[7:11], delta)
		return header[:11]
	}

	return header[:7]
}

func (chs *Chunks) writeHeaderShort(header []byte, delta uint32) []byte {
	d := delta
	if d > maxTime {
		d = maxTime
	}

	byteorder.BigEndian.PutUint24(header[:3], d)

	if d == maxTime {
		byteorder.BigEndian.PutUint32(header[3:7], delta)
		return header[:7]
	}

	return header[:3]
}

func (chs *Chunks) writeHeader(w io.Writer, message *message.Raw) (basic []byte, err error) {
	if message.ChunkID == 0 {
		message.ChunkID = 2
	}

	if message.Length == 0 {
		message.Length = uint32(len(message.Data))
	}

	newchunk := false

	var ch *Chunk

	if c, ok := chs.ChunkMap[message.ChunkID]; !ok {
		c = &Chunk{
			ID: message.ChunkID,
		}
		ch, chs.ChunkMap[message.ChunkID] = c, c
		newchunk = true
	} else {
		ch = c
	}

	if !message.Timestamp.IsZero() && message.Delta == 0 {
		message.Delta = message.Timestamp.Sub(chs.Timestamp.Add(time.Duration(ch.Timestamp) * time.Millisecond))
	} else {
		message.Timestamp = chs.Timestamp.Add(time.Duration(ch.Timestamp)*time.Millisecond + message.Delta)
	}

	delta := uint32(message.Delta.Milliseconds())
	timestamp := uint32(message.Timestamp.Sub(chs.Timestamp).Milliseconds())
	format := HeaderFormatFull

	if !newchunk {
		switch {
		case message.StreamID != ch.StreamID:
			format = HeaderFormatFull
		case message.Length != ch.Length || uint8(message.Type) != ch.TypeID:
			format = HeaderFormatMedium
		case delta != ch.Delta:
			format = HeaderFormatShort
		default:
			format = HeaderFormatNone
		}
	}

	var header []byte

	switch {
	case message.ChunkID > chunkLong:
		basic, header = chs.HeaderBuffer[:3], chs.HeaderBuffer[3:]
		basic[0] = (format << chunkBits) | chunkLongID

		byteorder.BigEndian.PutUint16(chs.HeaderBuffer[1:3], uint16(message.ChunkID-chunkOffset))
	case message.ChunkID > chunkMask:
		basic, header = chs.HeaderBuffer[:2], chs.HeaderBuffer[2:]
		basic[0] = format << chunkBits
		basic[1] = byte(message.ChunkID - chunkOffset)
	default:
		basic, header = chs.HeaderBuffer[:1], chs.HeaderBuffer[1:]
		basic[0] = (format << chunkBits) | byte(message.ChunkID)
	}

	switch format {
	case HeaderFormatFull:
		header = chs.writeHeaderFull(header, timestamp, message)
	case HeaderFormatMedium:
		header = chs.writeHeaderMedium(header, delta, message)
	case HeaderFormatShort:
		header = chs.writeHeaderShort(header, delta)
	case HeaderFormatNone:
		header = header[:0]
	}

	_, err = w.Write(chs.HeaderBuffer[:len(basic)+len(header)])

	return
}

// WriteMessage writes chunked message to given writer
func (chs *Chunks) WriteMessage(w io.Writer, message *message.Raw) (err error) {
	basic, err := chs.writeHeader(w, message)
	if err != nil {
		return err
	}

	data := message.Data
	for len(data) > 0 {
		var buf []byte
		if len(data) > len(chs.DataBuffer) {
			buf, data = data[:len(chs.DataBuffer)], data[len(chs.DataBuffer):]
		} else {
			buf, data = data, nil
		}

		_, err = w.Write(buf)
		if err != nil {
			return err
		}

		if len(data) > 0 {
			basic[0] = (3 << 6) | (basic[0] & chunkMask)

			_, err = w.Write(basic)
			if err != nil {
				return err
			}
		}
	}

	return
}

// Chunk represents state of RTMP chunk stream
type Chunk struct {
	ID        uint32
	TypeID    uint8
	Length    uint32
	StreamID  uint32
	Timestamp uint32
	Delta     uint32
	Data      bytes.Buffer
}

// Complete indicates that this chunk stream contains complete message
func (ch *Chunk) Complete() bool {
	return uint32(ch.Data.Len()) == ch.Length
}

// Bytes return message bytes
func (ch *Chunk) Bytes() []byte {
	return ch.Data.Bytes()
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
