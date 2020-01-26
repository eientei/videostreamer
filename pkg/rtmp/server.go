package rtmp

import (
	"context"
	"io"
	"net"

	"github.com/eientei/videostreamer/internal/contextio"
	"github.com/eientei/videostreamer/pkg/rtmp/connection"
	"github.com/eientei/videostreamer/pkg/rtmp/handshake"
)

// ServerOptions provide configuration for RTMP server
type ServerOptions struct {
	Connection *connection.Options
	Handshaker handshake.Handshaker
}

// Server provides RTMP server implementation
type Server struct {
	Listener    net.Listener
	Options     ServerOptions
	Context     context.Context
	Cancel      context.CancelFunc
	Connections chan Connection
}

func (srv *Server) handshake(conn io.ReadWriteCloser) {
	var err error

	rw := contextio.ReadWriter(srv.Context, conn)

	timestamp, peerDelta, err := srv.Options.Handshaker.Handshake(rw)
	if err != nil {
		_ = conn.Close()
		return
	}

	c := connection.NewConnection(srv.Context, conn, timestamp, peerDelta, srv.Options.Connection)

	select {
	case srv.Connections <- c:
	case <-srv.Context.Done():
		_ = conn.Close()
		return
	}
}

func (srv *Server) acceptLoop() {
	defer func() {
		_ = srv.Listener.Close()
		close(srv.Connections)
	}()

	for {
		conn, err := srv.Listener.Accept()
		if err != nil {
			srv.Cancel()
			return
		}

		go srv.handshake(conn)
	}
}

// Accept blocks until context is expired, or new RTMP connection is established
func (srv *Server) Accept(ctx context.Context) (conn Connection, err error) {
	select {
	case conn = <-srv.Connections:
		return
	case <-srv.Context.Done():
		return nil, srv.Context.Err()
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// NewServer returns new RTMP server using existing listener
func NewServer(parent context.Context, listener net.Listener, options *ServerOptions) *Server {
	if options == nil {
		options = &ServerOptions{}
	}

	if options.Handshaker == nil {
		options.Handshaker = handshake.NewServerKeysHandshake(nil)
	}

	ctx, cancel := context.WithCancel(parent)
	srv := &Server{
		Listener:    listener,
		Options:     *options,
		Context:     ctx,
		Cancel:      cancel,
		Connections: make(chan Connection),
	}

	go srv.acceptLoop()

	return srv
}

// NewServerListen returns new RTMP server creating TCP listener on provided endpoint
func NewServerListen(parent context.Context, addr string, options *ServerOptions) (srv *Server, err error) {
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return nil, err
	}

	srv = NewServer(parent, listener, options)

	return
}
