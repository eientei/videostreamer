// Package connection provides connection layer for sending and receiving RTMP messages
package connection

import (
	"context"
	"io"
	"time"

	"github.com/eientei/videostreamer/internal/contextio"
	"github.com/eientei/videostreamer/pkg/rtmp/chunk"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

// Options for connection
type Options struct {
	InboundMessageBuffer  int       // inbound message buffer size
	OutboundMessageBuffer int       // outbound message buffer size
	Timestamp             time.Time // connection epoch start
	PeerDelta             uint32    // peer time offset
}

// Connection provides contextualized and buffered receiving and sending of RTMP messages
type Connection struct {
	Options          Options
	RWC              io.ReadWriteCloser
	InboundChunks    *chunk.Chunks
	OutboundChunks   *chunk.Chunks
	Context          context.Context
	Cancel           context.CancelFunc
	InboundMessages  chan *message.Raw
	OutboundMessages chan *message.Raw
}

func (c *Connection) recvLoop() {
	reader := contextio.Reader(c.Context, c.RWC)

	defer func() {
		_ = c.RWC.Close()
		close(c.InboundMessages)
	}()

	for {
		msg, err := c.InboundChunks.ReadMessage(reader)
		if err != nil {
			c.Cancel()
			return
		}

		select {
		case c.InboundMessages <- msg:
		case <-c.Context.Done():
			return
		}
	}
}

func (c *Connection) sendLoop() {
	writer := contextio.Writer(c.Context, c.RWC)

	var msg *message.Raw

	defer func() {
		_ = c.RWC.Close()
		close(c.OutboundMessages)
	}()

	for {
		select {
		case msg = <-c.OutboundMessages:
		case <-c.Context.Done():
			return
		}

		err := c.OutboundChunks.WriteMessage(writer, msg)
		if err != nil {
			c.Cancel()
			return
		}
	}
}

// Send RTMP message within context
func (c *Connection) Send(ctx context.Context, msg *message.Raw) (err error) {
	select {
	case c.OutboundMessages <- msg:
		return nil
	case <-c.Context.Done():
		return c.Context.Err()
	case <-ctx.Done():
		return ctx.Err()
	}
}

// Recv RTMP message withing context
func (c *Connection) Recv(ctx context.Context) (msg *message.Raw, err error) {
	select {
	case msg = <-c.InboundMessages:
		return
	case <-c.Context.Done():
		return nil, c.Context.Err()
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// Close RTMP connection
func (c *Connection) Close() (err error) {
	c.Cancel()
	return c.RWC.Close()
}

// NewConnection returns new connection with parent context, ReadWriteCloser and connection options
func NewConnection(parent context.Context, rwc io.ReadWriteCloser, options *Options) *Connection {
	if options == nil {
		options = &Options{}
	}

	if options.Timestamp.IsZero() {
		options.Timestamp = time.Now()
	}

	ctx, cancel := context.WithCancel(parent)
	c := &Connection{
		Options:          *options,
		RWC:              rwc,
		InboundChunks:    chunk.NewChunks(options.Timestamp, options.PeerDelta),
		OutboundChunks:   chunk.NewChunks(options.Timestamp, options.PeerDelta),
		Context:          ctx,
		Cancel:           cancel,
		InboundMessages:  make(chan *message.Raw, options.InboundMessageBuffer),
		OutboundMessages: make(chan *message.Raw, options.OutboundMessageBuffer),
	}

	go c.recvLoop()
	go c.sendLoop()

	return c
}
