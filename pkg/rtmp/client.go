package rtmp

import (
	"context"
	"net"

	"github.com/eientei/videostreamer/internal/contextio"
	"github.com/eientei/videostreamer/pkg/rtmp/connection"
	"github.com/eientei/videostreamer/pkg/rtmp/handshake"
)

// ClientOptions provide configuration for RTMP client
type ClientOptions struct {
	Connection *connection.Options
	Handshaker handshake.Handshaker
}

// Dial established new connection with remote RTMP server
func Dial(parent context.Context, remote string, options *ClientOptions) (conn Connection, err error) {
	if options == nil {
		options = &ClientOptions{}
	}

	if options.Handshaker == nil {
		options.Handshaker = handshake.NewClientKeysHandshake(nil)
	}

	socket, err := net.Dial("tcp", remote)
	if err != nil {
		return nil, err
	}

	rw := contextio.ReadWriter(parent, socket)

	timestamp, peerDelta, err := options.Handshaker.Handshake(rw)
	if err != nil {
		return nil, err
	}

	conn = connection.NewConnection(parent, socket, timestamp, peerDelta, options.Connection)

	return
}
