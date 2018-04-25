package web

import (
	"crypto/sha1"
	"encoding/base64"
	"net"
	"net/http"
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
	Closed    bool
	Closer    chan struct{}
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
	Req    *http.Request
	Conn   net.Conn
	Closer chan struct{}
}

func (client *WssClient) Source() string {
	ip := client.Req.Header.Get("X-Real-IP")
	if ip != "" {
		return ip
	}
	return client.Conn.RemoteAddr().String()
}

func (client *WssClient) Send(data []byte) {
	if client.Closed {
		return
	}
	var header []byte
	if len(data) < 126 {
		header = []byte{1<<7 | 2, byte(len(data))}
	} else if len(data) < 65536 {
		header = []byte{1<<7 | 2, 126, byte(len(data) >> 8), byte(len(data))}
	} else {
		header = []byte{1<<7 | 2, 127,
			byte(len(data) >> 56),
			byte(len(data) >> 48),
			byte(len(data) >> 40),
			byte(len(data) >> 32),
			byte(len(data) >> 24),
			byte(len(data) >> 16),
			byte(len(data) >> 8),
			byte(len(data)),
		}
	}
	if _, err := client.Conn.Write(header); err != nil {
		client.Conn.Close()
		close(client.Closer)
		client.Closed = true
		return
	}
	if _, err := client.Conn.Write(data); err != nil {
		client.Conn.Close()
		close(client.Closer)
		client.Closed = true
		return
	}
}

func (client *WssClient) Close() {
	if client.Closed {
		return
	}
	client.Conn.Close()
	close(client.Closer)
	client.Closed = true
}

type Mp4Client struct {
	BaseClient
	Resp http.ResponseWriter
	Req  *http.Request
}

func (client *Mp4Client) Source() string {
	ip := client.Req.Header.Get("X-Real-IP")
	if ip != "" {
		return ip
	}
	return client.Req.RemoteAddr
}

func (client *Mp4Client) Send(data []byte) {
	if client.Closed {
		return
	}
	flusher := client.Resp.(http.Flusher)
	if _, err := client.Resp.Write(data); err != nil {
		close(client.Closer)
		client.Closed = true
	}
	flusher.Flush()
}

func (client *Mp4Client) Close() {
	if client.Closed {
		return
	}
	close(client.Closer)
	client.Closed = true
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

func (server *Server) ServeWss(resp http.ResponseWriter, req *http.Request, name string) {
	if conn, _, err := resp.(http.Hijacker).Hijack(); err != nil {
		http.Error(resp, err.Error(), http.StatusInternalServerError)
	} else {
		header := req.Header.Get("Sec-WebSocket-Key")
		concat := header + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
		sum := sha1.Sum([]byte(concat))
		accept := base64.StdEncoding.EncodeToString(sum[:])
		conn.Write([]byte("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n"))

		client := &WssClient{
			Conn:   conn,
			Req:    req,
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
	resp.Header().Add("Content-Type", "video/mp4")
	client := &Mp4Client{
		Resp: resp,
		Req:  req,
		BaseClient: BaseClient{
			Closer: make(chan struct{}),
		},
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
