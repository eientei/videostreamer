package server

import (
	"context"
	"net"
	"strconv"
	"io"
	"bytes"
)

type Client struct {
	Conn io.WriteCloser
	Init bool
	Sequence uint32
	AudioStartTime uint64
	VideoStartTime uint64
	Buffer *bytes.Buffer
}

type Stream struct {
	Name string
	Ftyp []byte
	Moov []byte
	First []byte
	Data chan *AVData
	Clients []*Client
}

type Server struct {
	RtmpPort uint16
	HttpPort uint16
	Context context.Context
	RtmpListener net.Listener
	RtmpDone chan struct{}
	HttpDone chan struct{}
	Streams map[string]*Stream
}

func NewServer(rtmp uint16, http uint16) *Server {
	return &Server{
		RtmpPort: rtmp,
		HttpPort: http,
		Streams: make(map[string]*Stream),
	}
}

func (server *Server) Serve(ctx context.Context) error {
	server.Context = ctx
	var err error
	server.RtmpListener, err = net.Listen("tcp", ":" + strconv.FormatUint(uint64(server.RtmpPort), 10))
	if err != nil {
		return err
	}
	server.RtmpDone = make(chan struct{})
	server.HttpDone = make(chan struct{})
	go RtmpServer(server)
	go HttpServer(":" + strconv.FormatUint(uint64(server.HttpPort), 10), server)
	return nil
}

func (server *Server) Wait() {
	<-server.Context.Done()
	server.RtmpListener.Close()
	<-server.RtmpDone
	<-server.HttpDone
}