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
)

const livePrefix = "/video/"
const wssSuffix = ".wss"
const mp4Suffix = ".mp4"

const (
	Disconnect  = 1
	Error       = 2
	Status      = 3
	Signup      = 4
	Published   = 5
	Auth        = 6
	UserDetails = 7
	Logout      = 8
)

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
		if client.Closed {
			return
		}
		client.Conn.Close()
		close(client.Closer)
		client.Closed = true
		return
	}
	if _, err := client.Conn.Write(data); err != nil {
		if client.Closed {
			return
		}
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

type EventMessage interface {
	Type() uint32
}

type TypedBytes struct {
	Type uint32          `json:"type"`
	Data json.RawMessage `json:"data"`
}

type ErrorMessage struct {
	Error string `json:"error"`
}

func (*ErrorMessage) Type() uint32 {
	return Error
}

type StatusMessage struct {
	Status string `json:"status"`
}

func (*StatusMessage) Type() uint32 {
	return Status
}

type DisconnectMessage struct {
}

func (*DisconnectMessage) Type() uint32 {
	return Disconnect
}

type SignupMessage struct {
	Username       string `json:"username"`
	Email          string `json:"email"`
	Password       string `json:"password"`
	PasswordRepeat string `json:"passwordrepeat"`
	Captcha        string `json:"captcha"`
	Ip             string `json:"-"`
}

func (*SignupMessage) Type() uint32 {
	return Signup
}

type PublishedMessage struct {
	Stream string `json:"stream"`
}

func (*PublishedMessage) Type() uint32 {
	return Published
}

type AuthMessage struct {
	Username string `json:"username"`
	Password string `json:"password"`
	Ip       string `json:"-"`
}

func (*AuthMessage) Type() uint32 {
	return Auth
}

type Stream struct {
	Name    string `json:"name"`
	Title   string `json:"title"`
	Key     string `json:"key"`
	Logourl string `json:"logourl"`
}

type UserDetailsMessage struct {
	Username      string    `json:"username"`
	Email         string    `json:"email"`
	Gravatar      string    `json:"gravatar"`
	Streams       []*Stream `json:"streams"`
	Notifications []string  `json:"notifications"`
}

func (*UserDetailsMessage) Type() uint32 {
	return UserDetails
}

type LogoutMessage struct {
}

func (*LogoutMessage) Type() uint32 {
	return Logout
}

type EventClient interface {
	Read() EventMessage
	Send(message EventMessage)
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
		fmt.Println("closed")
		return &DisconnectMessage{}
	}
	data := make([]byte, 0)
	basic := make([]byte, 2)
	if _, err := io.ReadFull(client.Conn, basic); err != nil {
		fmt.Println("ra", err)
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
	if basic[0]&0xf != 1 || (basic[1]>>7)&1 == 0 {
		fmt.Println("rb")
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
	l := uint64(basic[1] & 0x7f)
	if l == 126 {
		lenbuf := make([]byte, 2)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			fmt.Println("rc")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
	} else if l == 127 {
		lenbuf := make([]byte, 8)
		if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
			fmt.Println("rh")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
	}
	mask := make([]byte, 4)
	if _, err := io.ReadFull(client.Conn, mask); err != nil {
		fmt.Println("rj")
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
	fin := (basic[0] >> 7) == 1
	ndata := make([]byte, l)
	if _, err := io.ReadFull(client.Conn, ndata); err != nil {
		fmt.Println("rk")
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
	for i, c := range ndata {
		ndata[i] = c ^ mask[i%4]
	}
	data = append(data, ndata...)
	for !fin {
		if _, err := io.ReadFull(client.Conn, basic); err != nil {
			fmt.Println("rl")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		if basic[0]&0xf != 0 || (basic[1]>>7)&1 == 0 {
			fmt.Println("rm")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		l = uint64(basic[1] & 0x7f)
		if l == 126 {
			lenbuf := make([]byte, 2)
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				fmt.Println("rn")
				close(client.Closer)
				client.Closed = true
				return &DisconnectMessage{}
			}
			l = uint64(lenbuf[0])<<8 | uint64(lenbuf[1])
		} else if l == 127 {
			lenbuf := make([]byte, 8)
			fmt.Println("ro")
			if _, err := io.ReadFull(client.Conn, lenbuf); err != nil {
				close(client.Closer)
				client.Closed = true
				return &DisconnectMessage{}
			}
			l = uint64(lenbuf[0])<<56 | uint64(lenbuf[1])<<48 | uint64(lenbuf[2])<<40 | uint64(lenbuf[3])<<32 | uint64(lenbuf[4])<<24 | uint64(lenbuf[5])<<16 | uint64(lenbuf[6])<<8 | uint64(lenbuf[7])
		}
		if _, err := io.ReadFull(client.Conn, mask); err != nil {
			fmt.Println("rp")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		fin = (basic[0] >> 7) == 1
		ndata := make([]byte, l)
		if _, err := io.ReadFull(client.Conn, ndata); err != nil {
			fmt.Println("rq")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		for i, c := range ndata {
			ndata[i] = c ^ mask[i%4]
		}
		data = append(data, ndata...)
	}

	typed := &TypedBytes{}
	if err := json.Unmarshal(data, typed); err != nil {
		fmt.Println("rr", err)
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
	switch typed.Type {
	case Signup:
		signup := &SignupMessage{}
		if err := json.Unmarshal(typed.Data, signup); err != nil {
			fmt.Println(err, string(typed.Data))
			fmt.Println("rs")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		signup.Ip = client.Req.Header.Get("X-Real-IP")
		if signup.Ip == "" {
			signup.Ip = client.Req.RemoteAddr[:strings.Index(client.Req.RemoteAddr, ":")]
		}
		return signup
	case Auth:
		auth := &AuthMessage{}
		if err := json.Unmarshal(typed.Data, auth); err != nil {
			fmt.Println(err, string(typed.Data))
			fmt.Println("rs")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		auth.Ip = client.Req.Header.Get("X-Real-IP")
		if auth.Ip == "" {
			auth.Ip = client.Req.RemoteAddr[:strings.Index(client.Req.RemoteAddr, ":")]
		}
		return auth
	case Logout:
		logout := &LogoutMessage{}
		if err := json.Unmarshal(typed.Data, logout); err != nil {
			fmt.Println(err, string(typed.Data))
			fmt.Println("rs")
			close(client.Closer)
			client.Closed = true
			return &DisconnectMessage{}
		}
		return logout
	default:
		fmt.Println("rt")
		close(client.Closer)
		client.Closed = true
		return &DisconnectMessage{}
	}
}

func (client *WsEventeer) Send(message EventMessage) {
	if client.Closed {
		return
	}

	if msgdata, err := json.Marshal(message); err != nil {
		close(client.Closer)
		client.Closed = true
	} else {
		typed := &TypedBytes{
			Type: message.Type(),
			Data: msgdata,
		}
		if data, err := json.Marshal(typed); err != nil {
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
				close(client.Closer)
				client.Closed = true
				return
			}
			if _, err := client.Conn.Write(data); err != nil {
				if client.Closed {
					return
				}
				close(client.Closer)
				client.Closed = true
				return
			}
		}
	}
	if message.Type() == Disconnect {
		close(client.Closer)
		client.Closed = true
		return
	}
}

type ClientHandler interface {
	ClientConnect(client Client, path string, name string) bool
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

func (server *Server) ServeWss(resp http.ResponseWriter, req *http.Request, name string) {
	if conn, err := WsHandshake(resp, req); err != nil {
		http.Error(resp, err.Error(), http.StatusInternalServerError)
		return
	} else {
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
		client := &WssClient{
			Conn:   conn,
			Req:    req,
			Closer: make(chan struct{}),
		}
		for _, h := range server.ClientHandlers {
			if !h.ClientConnect(client, path, name) {
				conn.Close()
				return
			}
		}
		<-client.Closer
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
		if !h.ClientConnect(client, path, name) {
			return
		}
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
				name := next[:len(next)-len(wssSuffix)]
				server.ServeWss(resp, req, name)
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
