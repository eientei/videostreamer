package connection

import (
	"context"
	"errors"
	"io"
	"sync"

	"github.com/eientei/videostreamer/internal/contextio"
	"github.com/eientei/videostreamer/pkg/rtmp/chunk"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

var (
	// ErrConnectionClosed indicates operation on closed connection
	ErrConnectionClosed = errors.New("connection closed")
)

// Connection provides contextualized and buffered receiving and sending of RTMP messages
type Connection struct {
	options          Options
	rwc              io.ReadWriteCloser
	inboundChunks    *chunk.Chunks
	outboundChunks   *chunk.Chunks
	inboundMessages  chan *message.Raw
	outboundMessages chan *message.Raw
	context          context.Context
	cancel           context.CancelFunc
	mutex            *sync.Mutex
	err              error
}

func (c *Connection) recvLoop() {
	reader := contextio.Reader(c.context, c.rwc)

	for {
		msg, err := c.inboundChunks.ReadMessage(reader)
		if err != nil {
			c.err = err
			close(c.inboundMessages)
			_ = c.Close()

			return
		}

		select {
		case c.inboundMessages <- msg:
		case <-c.context.Done():
			c.err = err
			close(c.inboundMessages)
			_ = c.Close()

			return
		}
	}
}

func (c *Connection) sendLoop() {
	writer := contextio.Writer(c.context, c.rwc)

	for msg := range c.outboundMessages {
		err := c.outboundChunks.WriteMessage(writer, msg)
		if err != nil {
			c.err = err
			_ = c.Close()
		}
	}
}

// Send RTMP message within context
func (c *Connection) Send(ctx context.Context, msg *message.Raw) (err error) {
	c.mutex.Lock()

	if err = c.err; err == nil {
		select {
		case c.outboundMessages <- msg:
			err = c.err
		case <-c.context.Done():
			err = c.context.Err()
		case <-ctx.Done():
			err = ctx.Err()
		}
	}

	c.mutex.Unlock()

	return
}

// Recv RTMP message withing context
func (c *Connection) Recv(ctx context.Context) (msg *message.Raw, err error) {
	select {
	case msg = <-c.inboundMessages:
		err = c.err
	case <-c.context.Done():
		err = c.context.Err()
	case <-ctx.Done():
		err = ctx.Err()
	}

	return
}

// Close RTMP connection
func (c *Connection) Close() (err error) {
	c.mutex.Lock()

	if c.err == nil {
		c.err = ErrConnectionClosed
		err = c.rwc.Close()
		c.cancel()
	}

	c.mutex.Unlock()

	return
}
