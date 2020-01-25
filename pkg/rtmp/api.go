// Package rtmp provides client and server implementation for RTMP message and chunk streams
package rtmp

import (
	"context"

	"github.com/eientei/videostreamer/pkg/rtmp/message"
)

// Connection interface provides RTMP connection methods for terminating connection and writing and reading messages
type Connection interface {
	Recv(ctx context.Context) (*message.Raw, error)
	Send(ctx context.Context, msg *message.Raw) error
	Close() error
}
