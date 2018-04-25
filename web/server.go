package web

import (
	"net/http"

	"github.com/gorilla/websocket"
)

const livePrefix = "/live/"
const wssSuffix = ".wss"
const mp4Suffix = ".mp4"

type Client interface {
	Sequence() uint32
	Atime() uint64
	Vtime() uint64
	Advance(seq uint32, atime uint64, vtime uint64)
	Source() string
	Send(data []byte)
	Close()
}

type BaseClient struct {
	SequenceI uint32
	AtimeI    uint64
	VtimeI    uint64
}

func (client *BaseClient) Sequence() uint32 {
	return client.SequenceI
}

func (client *BaseClient) Atime() uint64 {
	return client.AtimeI
}

func (client *BaseClient) Vtime() uint64 {
	return client.VtimeI
}

func (client *BaseClient) Advance(seq uint32, atime uint64, vtime uint64) {
	client.SequenceI += seq
	client.AtimeI += atime
	client.VtimeI += vtime
}

type WssClient struct {
	BaseClient
	Conn   *websocket.Conn
	Closer chan struct{}
}

func (client *WssClient) Source() string {
	return client.Conn.RemoteAddr().String()
}

func (client *WssClient) Send(data []byte) {
	if err := client.Conn.WriteMessage(websocket.BinaryMessage, data); err != nil {
		client.Conn.Close()
		close(client.Closer)
	}
}

func (client *WssClient) Close() {
	client.Conn.Close()
	close(client.Closer)
}

type Mp4Client struct {
	BaseClient
	Resp   http.ResponseWriter
	Req    *http.Request
	Closer chan struct{}
}

func (client *Mp4Client) Source() string {
	return client.Req.RemoteAddr
}

func (client *Mp4Client) Send(data []byte) {
	flusher := client.Resp.(http.Flusher)
	if _, err := client.Resp.Write(data); err != nil {
		close(client.Closer)
	}
	flusher.Flush()
}

func (client *Mp4Client) Close() {
	close(client.Closer)
}

type ClientHandler interface {
	ClientConnect(client Client, name string) bool
	ClientDisconnect(client Client, name string)
}

type Server struct {
	Config         *Config
	ClientHandlers []ClientHandler
}

func NewServer(config *Config) *Server {
	return &Server{
		Config: config,
	}
}

func (server *Server) Subscribe(handler ClientHandler) {
	server.ClientHandlers = append(server.ClientHandlers, handler)
}

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
	EnableCompression: false,
}

func (server *Server) ServeWss(resp http.ResponseWriter, req *http.Request, name string) {
	if conn, err := upgrader.Upgrade(resp, req, nil); err != nil {
		http.Error(resp, err.Error(), http.StatusInternalServerError)
		return
	} else {
		client := &WssClient{
			Conn:   conn,
			Closer: make(chan struct{}),
		}
		for _, h := range server.ClientHandlers {
			if !h.ClientConnect(client, name) {
				conn.Close()
				return
			}
		}
		<-client.Closer
		for _, h := range server.ClientHandlers {
			h.ClientDisconnect(client, name)
		}
	}
}

func (server *Server) ServeMp4(resp http.ResponseWriter, req *http.Request, name string) {
	client := &Mp4Client{
		Resp:   resp,
		Req:    req,
		Closer: make(chan struct{}),
	}
	for _, h := range server.ClientHandlers {
		if !h.ClientConnect(client, name) {
			return
		}
	}
	<-client.Closer
	for _, h := range server.ClientHandlers {
		h.ClientDisconnect(client, name)
	}
}

func (server *Server) ServeHTTP(resp http.ResponseWriter, req *http.Request) {
	switch req.URL.Path {
	default:
		if len(req.URL.Path) > len(livePrefix) && req.URL.Path[:len(livePrefix)] == livePrefix {
			next := req.URL.Path[len(livePrefix):]
			if len(next) > len(wssSuffix) && next[len(next)-len(wssSuffix):] == wssSuffix {
				name := next[:len(next)-len(wssSuffix)]
				server.ServeWss(resp, req, name)
			} else if len(next) > len(mp4Suffix) && next[len(next)-len(mp4Suffix):] == mp4Suffix {
				name := next[:len(next)-len(mp4Suffix)]
				server.ServeMp4(resp, req, name)
			}
		}
	}
	req.Body.Close()
}

func (server *Server) ListenAndServe() {
	s := &http.Server{
		Addr:    server.Config.Address,
		Handler: server,
	}
	s.ListenAndServe()
}
