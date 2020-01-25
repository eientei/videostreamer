// Package message provides handling RTMP messages
package message

import "time"

// Type message type
type Type uint8

// Header for message
type Header struct {
	ChunkID   uint32
	Type      Type
	Timestamp time.Time
	Delta     time.Duration
	Length    uint32
	StreamID  uint32
}

// Raw message
type Raw struct {
	Header
	Data []byte
}
