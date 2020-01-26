// Package chunk provides reading and writing messages using RTMP chunk streams
package chunk

import (
	"time"
)

// Header format codes
const (
	HeaderFormatFull   byte = 0x00
	HeaderFormatMedium byte = 0x01
	HeaderFormatShort  byte = 0x02
	HeaderFormatNone   byte = 0x03
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
