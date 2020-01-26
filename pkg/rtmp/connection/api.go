// Package connection provides connection layer for sending and receiving RTMP messages
package connection

import (
	"context"
	"io"
	"sync"
	"time"

	"github.com/eientei/videostreamer/pkg/rtmp/chunk"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

// Options for connection
type Options struct {
	InboundMessageBuffer  int // inbound message buffer size
	OutboundMessageBuffer int // outbound message buffer size
}

// NewConnection returns new connection with parent context, ReadWriteCloser in state after completed handshake and
// connection options
func NewConnection(
	parent context.Context,
	rwc io.ReadWriteCloser,
	timestamp time.Time,
	peerDelta uint32,
	options *Options,
) *Connection {
	if options == nil {
		options = &Options{}
	}

	if timestamp.IsZero() {
		timestamp = time.Now()
	}

	ctx, cancel := context.WithCancel(parent)
	c := &Connection{
		options:          *options,
		rwc:              rwc,
		inboundChunks:    chunk.NewChunks(timestamp, peerDelta),
		outboundChunks:   chunk.NewChunks(timestamp, peerDelta),
		context:          ctx,
		cancel:           cancel,
		inboundMessages:  make(chan *message.Raw, options.InboundMessageBuffer),
		outboundMessages: make(chan *message.Raw, options.OutboundMessageBuffer),
		mutex:            &sync.Mutex{},
	}

	go c.recvLoop()
	go c.sendLoop()

	return c
}
