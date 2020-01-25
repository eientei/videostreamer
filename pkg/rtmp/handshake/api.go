// Package handshake provides handshaker interface and default implementations for RTMP handshake stage
package handshake

import (
	"io"
	"time"
)

// Handshaker provides method to perform RTMP handshake with given io.ReadWriter,
// returning timestamp of connection epoch, peer time beginning point or possible error
type Handshaker interface {
	Handshake(rw io.ReadWriter) (timestamp time.Time, peerDelta uint32, err error)
}
