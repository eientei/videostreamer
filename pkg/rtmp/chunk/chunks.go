package chunk

import (
	"io"
	"time"

	"github.com/eientei/videostreamer/internal/byteorder"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

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

	format = chs.HeaderBuffer[0] >> 6
	chunkID := uint32(chs.HeaderBuffer[0]) & 0x3f

	switch chunkID {
	case 0:
		_, err = io.ReadFull(r, chs.HeaderBuffer[:1])
		if err != nil {
			return
		}

		chunkID = uint32(chs.HeaderBuffer[0]) + 0x40
	case 1:
		_, err = io.ReadFull(r, chs.HeaderBuffer[:2])
		if err != nil {
			return
		}

		chunkID = uint32(byteorder.BigEndian.Uint16(chs.HeaderBuffer)) + 0x40
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

	if ch.Timestamp == 0xFFFFFF {
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

	if ch.Delta == 0xFFFFFF {
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

	if ch.Delta == 0xFFFFFF {
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
	if t > 0xFFFFFF {
		t = 0xFFFFFF
	}

	byteorder.BigEndian.PutUint24(header[:3], t)
	byteorder.BigEndian.PutUint24(header[3:6], message.Length)
	header[6] = uint8(message.Type)
	byteorder.BigEndian.PutUint32(header[7:11], message.StreamID)

	if t == 0xFFFFFF {
		byteorder.BigEndian.PutUint32(header[11:15], timestamp)
		return header[:15]
	}

	return header[:11]
}

func (chs *Chunks) writeHeaderMedium(header []byte, delta uint32, message *message.Raw) []byte {
	d := delta
	if d > 0xFFFFFF {
		d = 0xFFFFFF
	}

	byteorder.BigEndian.PutUint24(header[:3], d)
	byteorder.BigEndian.PutUint24(header[3:6], message.Length)
	header[6] = uint8(message.Type)

	if d == 0xFFFFFF {
		byteorder.BigEndian.PutUint32(header[7:11], delta)
		return header[:11]
	}

	return header[:7]
}

func (chs *Chunks) writeHeaderShort(header []byte, delta uint32) []byte {
	d := delta
	if d > 0xFFFFFF {
		d = 0xFFFFFF
	}

	byteorder.BigEndian.PutUint24(header[:3], d)

	if d == 0xFFFFFF {
		byteorder.BigEndian.PutUint32(header[3:7], delta)
		return header[:7]
	}

	return header[:3]
}

func (chs *Chunks) sanitizeMessage(message *message.Raw) (format byte, timestamp, delta uint32) {
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

	delta = uint32(message.Delta.Milliseconds())
	timestamp = uint32(message.Timestamp.Sub(chs.Timestamp).Milliseconds())
	format = HeaderFormatFull

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

	return
}

func (chs *Chunks) writeHeader(w io.Writer, message *message.Raw) (basic []byte, err error) {
	format, timestamp, delta := chs.sanitizeMessage(message)

	var header []byte

	switch {
	case message.ChunkID > 0x13f:
		basic, header = chs.HeaderBuffer[:3], chs.HeaderBuffer[3:]
		basic[0] = (format << 6) | 1

		byteorder.BigEndian.PutUint16(chs.HeaderBuffer[1:3], uint16(message.ChunkID-0x40))
	case message.ChunkID > 0x3f:
		basic, header = chs.HeaderBuffer[:2], chs.HeaderBuffer[2:]
		basic[0] = format << 6
		basic[1] = byte(message.ChunkID - 0x40)
	default:
		basic, header = chs.HeaderBuffer[:1], chs.HeaderBuffer[1:]
		basic[0] = (format << 6) | byte(message.ChunkID)
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
			basic[0] = (3 << 6) | (basic[0] & 0x3f)

			_, err = w.Write(basic)
			if err != nil {
				return err
			}
		}
	}

	return
}
