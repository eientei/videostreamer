package web

import (
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"os"
	"strings"
	"time"
)

const livePrefix = "/video/"
const wssSuffix = ".wss"
const mp4Suffix = ".mp4"

type Client interface {
	Sequence() uint32
	Atime() uint64
	Vtime() uint64
	Init(atime uint64, vtime uint64)
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

func (client *BaseClient) Init(atime uint64, vtime uint64) {
	client.AtimeI = atime
	client.VtimeI = vtime
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
	return client.Conn.RemoteAddr().String()[:strings.Index(client.Conn.RemoteAddr().String(), ":")]
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
		client.Close()
		return
	}
	if _, err := client.Conn.Write(data); err != nil {
		client.Close()
		return
	}
}

func (client *WssClient) Close() {
	if client.Closed {
		return
	}
	client.Closed = true
	client.Conn.Write([]byte{1<<7 | 8, 0})
	client.Conn.Close()
	close(client.Closer)
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
	return client.Req.RemoteAddr[:strings.Index(client.Req.RemoteAddr, ":")]
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

type EventClient interface {
	Read() EventMessage
	Send(message EventMessage)
	Ip() string
}

type WsEventeer struct {
	Conn   net.Conn
	Closer chan struct{}
	Closed bool
	Req    *http.Request
}

func (client *WsEventeer) Close() {
	client.Conn.Write([]byte{1<<7 | 8, 0})
}

func (client *WsEventeer) Read() EventMessage {
	if client.Closed {
		return nil
	}
	data := make([]byte, 0)
	basic := make([]byte, 2)
	if _, err := io.ReadFull(client.Conn, basic); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}
	if basic[0]&0xf != 1 || (basic[1]>>7)&1 == 0 {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}
	l := uint64(basic[1] & 0x7f)
	if l == 126 {
		lenbuf := make([]byte, 2)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
	} else if l == 127 {
		lenbuf := make([]byte, 8)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
	}
	mask := make([]byte, 4)
	if _, err := io.ReadFull(client.Conn, mask); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}
	fin := (basic[0] >> 7) == 1
	ndata := make([]byte, l)
	if _, err := io.ReadFull(client.Conn, ndata); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}
	for i, c := range ndata {
		ndata[i] = c ^ mask[i%4]
	}
	data = append(data, ndata...)
	for !fin {
		if _, err := io.ReadFull(client.Conn, basic); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		if basic[0]&0xf != 0 || (basic[1]>>7)&1 == 0 {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		l = uint64(basic[1] & 0x7f)
		if l == 126 {
			lenbuf := make([]byte, 2)
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				client.Conn.Write([]byte{1<<7 | 8, 0})
				close(client.Closer)
				client.Closed = true
				return nil
			}
			l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
		} else if l == 127 {
			lenbuf := make([]byte, 8)
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				client.Conn.Write([]byte{1<<7 | 8, 0})
				close(client.Closer)
				client.Closed = true
				return nil
			}
			l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
		}
		if _, err := io.ReadFull(client.Conn, mask); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		fin = (basic[0] >> 7) == 1
		ndata := make([]byte, l)
		if _, err := io.ReadFull(client.Conn, ndata); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
			return nil
		}
		for i, c := range ndata {
			ndata[i] = c ^ mask[i%4]
		}
		data = append(data, ndata...)
	}

	typed := &TypedBytes{}
	if err := json.Unmarshal(data, typed); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}

	var msg EventMessage
	switch typed.Type {
	case UserSignup:
		msg = &UserSignupMessage{}
	case UserLogin:
		msg = &UserLoginMessage{}
	case UserLogout:
		msg = &UserLogoutMessage{}
	case UserInfoUpdate:
		msg = &UserInfoUpdateMessage{}
	case StreamInfoUpdate:
		msg = &StreamInfoUpdateMessage{}
	case StreamKeyUpdate:
		msg = &StreamKeyUpdateMessage{}
	case StreamPrivatedUpdate:
		msg = &StreamPrivatedUpdateMessage{}
	case StreamDelete:
		msg = &StreamDeleteMessage{}
	case StreamAdd:
		msg = &StreamAddMessage{}
	case StreamSubscribe:
		msg = &StreamSubscribeMessage{}
	case StreamUnsubscribe:
		msg = &StreamUnsubscribeMessage{}
	case StreamList:
		msg = &StreamListMessage{}
	case StreamInfoReq:
		msg = &StreamInfoReqMessage{}
	case MessageSend:
		msg = &MessageSendMessage{}
	case MessageEdit:
		msg = &MessageEditMessage{}
	case MessageDelete:
		msg = &MessageDeleteMessage{}
	case MessageHistory:
		msg = &MessageHistoryMessage{}
	}

	if msg == nil {
		return nil
	}

	if err := json.Unmarshal(typed.Data, msg); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return nil
	}

	return msg
}

func WssPing(client *WssClient) {
	for {
		time.Sleep(20 * time.Second)
		_, err := client.Conn.Write([]byte{1<<7 | 9, 0})
		if err != nil {
			return
		}
	}
}

func WssRead(client *WssClient) {
	if client.Closed {
		return
	}
	data := make([]byte, 0)
	basic := make([]byte, 2)
	if _, err := io.ReadFull(client.Conn, basic); err != nil {
		client.Close()
		return
	}
	if (basic[0]&0xf != 1 && basic[0]&0xf != 10) || (basic[1]>>7)&1 == 0 {
		fmt.Println("RECV", basic[0]&0xf)
		client.Close()
		return
	}
	l := uint64(basic[1] & 0x7f)
	if l == 126 {
		lenbuf := make([]byte, 2)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			client.Close()
			return
		}
		l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
	} else if l == 127 {
		lenbuf := make([]byte, 8)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			client.Close()
			return
		}
		l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
	}
	mask := make([]byte, 4)
	if _, err := io.ReadFull(client.Conn, mask); err != nil {
		client.Close()
		return
	}
	fin := (basic[0] >> 7) == 1
	ndata := make([]byte, l)
	if _, err := io.ReadFull(client.Conn, ndata); err != nil {
		client.Close()
		return
	}
	for i, c := range ndata {
		ndata[i] = c ^ mask[i%4]
	}
	data = append(data, ndata...)
	for !fin {
		if _, err := io.ReadFull(client.Conn, basic); err != nil {
			client.Close()
			return
		}
		if basic[0]&0xf != 0 || (basic[1]>>7)&1 == 0 {
			client.Close()
			return
		}
		l = uint64(basic[1] & 0x7f)
		if l == 126 {
			lenbuf := make([]byte, 2)
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				client.Close()
				return
			}
			l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
		} else if l == 127 {
			lenbuf := make([]byte, 8)
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				client.Close()
				return
			}
			l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
		}
		if _, err := io.ReadFull(client.Conn, mask); err != nil {
			client.Close()
			return
		}
		fin = (basic[0] >> 7) == 1
		ndata := make([]byte, l)
		if _, err := io.ReadFull(client.Conn, ndata); err != nil {
			client.Close()
			return
		}
		for i, c := range ndata {
			ndata[i] = c ^ mask[i%4]
		}
		data = append(data, ndata...)
	}
}

func (client *WsEventeer) Send(message EventMessage) {
	if client.Closed {
		return
	}

	if msgdata, err := json.Marshal(message); err != nil {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
	} else {
		typed := &TypedBytes{
			Type: message.Type(),
			Data: msgdata,
		}
		if data, err := json.Marshal(typed); err != nil {
			client.Conn.Write([]byte{1<<7 | 8, 0})
			close(client.Closer)
			client.Closed = true
		} else {

			var header []byte
			if len(data) < 126 {
				header = []byte{1<<7 | 1, byte(len(data))}
			} else if len(data) < 65536 {
				header = []byte{1<<7 | 1, 126, byte(len(data) >> 8), byte(len(data))}
			} else {
				header = []byte{1<<7 | 1, 127,
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
				if client.Closed {
					return
				}
				client.Conn.Write([]byte{1<<7 | 8, 0})
				close(client.Closer)
				client.Closed = true
				return
			}
			if _, err := client.Conn.Write(data); err != nil {
				if client.Closed {
					return
				}
				client.Conn.Write([]byte{1<<7 | 8, 0})
				close(client.Closer)
				client.Closed = true
				return
			}
		}
	}
	if message.Type() == Disconnect {
		client.Conn.Write([]byte{1<<7 | 8, 0})
		close(client.Closer)
		client.Closed = true
		return
	}
}

func (client *WsEventeer) Ip() string {
	ip := client.Req.Header.Get("X-Real-IP")
	if ip != "" {
		return ip
	}
	return client.Conn.RemoteAddr().String()
}

type ClientHandler interface {
	ClientOk(path string, name string) bool
	ClientConnect(client Client, path string, name string)
	ClientDisconnect(client Client, path string, name string)
	EventeerConnect(client EventClient)
	EventeerDisconnect(client EventClient)
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

func (server *Server) ServeWss(resp http.ResponseWriter, req *http.Request, client *WssClient, path string, name string) {
	if conn, err := WsHandshake(resp, req); err != nil {
		http.Error(resp, err.Error(), http.StatusInternalServerError)
		return
	} else {
		client.Conn = conn
		for _, h := range server.ClientHandlers {
			h.ClientConnect(client, path, name)
		}
		go WssRead(client)
		go WssPing(client)

		<-client.Closer
		/*
			for {
				select {
				case <-client.Closer:
					break
				case <-time.After(20 * time.Second):
					conn.Write([]byte{1<<7 | 9, 0})
				}
			}
		*/
		/*
			for {
				fmt.Println("next")
				select {
				case <-client.Closer:
					break
				case <-time.After(20 * time.Second):
					conn.Write([]byte{1<<7 | 9, 0})
				}
			}
		*/
		for _, h := range server.ClientHandlers {
			h.ClientDisconnect(client, path, name)
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
	path := ""
	for i, c := range name {
		if c == '/' {
			if path == "" {
				path = name[:i]
				name = name[i+1:]
			} else {
				name = name[:i]
				break
			}
		}
	}
	for _, h := range server.ClientHandlers {
		if !h.ClientOk(path, name) {
			return
		}
		h.ClientConnect(client, path, name)
	}
	<-client.Closer
	for _, h := range server.ClientHandlers {
		h.ClientDisconnect(client, path, name)
	}
}

func WsHandshake(resp http.ResponseWriter, req *http.Request) (net.Conn, error) {
	if conn, _, err := resp.(http.Hijacker).Hijack(); err != nil {
		return nil, err
	} else {
		header := req.Header.Get("Sec-WebSocket-Key")
		concat := header + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
		sum := sha1.Sum([]byte(concat))
		accept := base64.StdEncoding.EncodeToString(sum[:])
		conn.Write([]byte("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n"))
		return conn, nil
	}
}

func (server *Server) ServeEvent(resp http.ResponseWriter, req *http.Request) {
	if conn, err := WsHandshake(resp, req); err != nil {
		http.Error(resp, err.Error(), http.StatusInternalServerError)
		return
	} else {
		client := &WsEventeer{
			Conn:   conn,
			Closer: make(chan struct{}),
			Req:    req,
		}
		for _, h := range server.ClientHandlers {
			h.EventeerConnect(client)
		}

		<-client.Closer
		client.Close()
		client.Conn.Close()
		for _, h := range server.ClientHandlers {
			h.EventeerDisconnect(client)
		}
	}
}

func (server *Server) ServeHTTP(resp http.ResponseWriter, req *http.Request) {
	switch req.URL.Path {
	case "/api/event":
		server.ServeEvent(resp, req)
	default:
		if len(req.URL.Path) > len(livePrefix) && req.URL.Path[:len(livePrefix)] == livePrefix {
			next := req.URL.Path[len(livePrefix):]
			if len(next) > len(wssSuffix) && next[len(next)-len(wssSuffix):] == wssSuffix {
				path := ""
				name := ""
				n := 0
				full := next[:len(next)-len(wssSuffix)]
				for i, c := range full {
					if c == '/' {
						if path == "" {
							path = full[:i]
							name = full[i+1:]
							n = i + 1
						} else {
							name = full[n:i]
							break
						}
					}
				}
				if n == 0 {
					path = full
				}
				client := &WssClient{
					Req:    req,
					Closer: make(chan struct{}),
				}
				for _, h := range server.ClientHandlers {
					if !h.ClientOk(path, name) {
						req.Body.Close()
						return
					}
				}

				server.ServeWss(resp, req, client, path, name)
			} else if len(next) > len(mp4Suffix) && next[len(next)-len(mp4Suffix):] == mp4Suffix {
				name := next[:len(next)-len(mp4Suffix)]
				server.ServeMp4(resp, req, name)
			}
		} else {
			name := "./static/build/" + req.URL.Path
			if _, err := os.Stat(name); os.IsNotExist(err) {
				http.ServeFile(resp, req, "./static/build/index.html")
			} else {
				http.ServeFile(resp, req, name)
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
