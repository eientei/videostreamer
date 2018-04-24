package http

import (
	"bytes"
	"crypto/sha1"
	"encoding/base64"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"strconv"

	"../server"
)

var NotFound = errors.New("not found")

const prefix = "/live/"
const fileSuffix = ".mp4"
const wssSuffix = ".wss"
const websockHeader = "Sec-WebSocket-Key"

/*
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type ConnWriter struct {
	Logger *log.Logger
	Conn   *websocket.Conn
	Chan   chan []byte
	Err    error
}

type SignalWriter struct {
	Logger *log.Logger
	Conn   *websocket.Conn
}

func (cw *SignalWriter) Write(data []byte) (int, error) {
	return len(data), nil
}

func (cw *SignalWriter) Close() error {
	cw.Logger.Println("wss:// client disconnected", cw.Conn.RemoteAddr().String())
	return cw.Conn.Close()
}

func (cw *ConnWriter) Write(data []byte) (int, error) {
	cw.Chan <- data
	return len(data), cw.Err
}

func (cw *ConnWriter) Close() error {
	cw.Logger.Println("wss:// client disconnected", cw.Conn.RemoteAddr().String())
	close(cw.Chan)
	return cw.Conn.Close()
}

type HttpWriter struct {
	Conn   http.ResponseWriter
	Closer chan struct{}
	Chan   chan []byte
	Err    error
}

func (cw *HttpWriter) Write(data []byte) (int, error) {
	cw.Chan <- data
	return len(data), cw.Err
}

func (cw *HttpWriter) Close() error {
	close(cw.Closer)
	return nil
}

func WebsocketHandler(context *server.Context) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		if stream, ok := vars["stream"]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata, ok := context.Streams[stream]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata.ContainerInit == nil {
			http.Error(w, "Try again a bit later", http.StatusPartialContent)
			return
		} else {
			if conn, err := upgrader.Upgrade(w, r, nil); err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
			} else {
				sdata.Logger.Println("wss:// client connected", r.RemoteAddr)
				cw := &ConnWriter{sdata.Logger, conn, make(chan []byte, 8), nil}
				go func() {
					for {
						msg, ok := <-cw.Chan
						if !ok {
							return
						}
						cw.Err = cw.Conn.WriteMessage(websocket.BinaryMessage, msg)
					}
				}()
				cw.Write(sdata.ContainerInit)
				sdata.Clients = append(sdata.Clients, &server.Client{Conn: cw})
			}
		}
	}
}

func SignalHandler(context *server.Context) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		if stream, ok := vars["stream"]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata, ok := context.Streams[stream]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata.ContainerInit == nil {
			http.Error(w, "Try again a bit later", http.StatusPartialContent)
			return
		} else {
			if conn, err := upgrader.Upgrade(w, r, nil); err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
			} else {
				sdata.Logger.Println("signal:// client connected", r.RemoteAddr)
				cw := &SignalWriter{sdata.Logger, conn}
				cw.Write(sdata.ContainerInit)
				sdata.Clients = append(sdata.Clients, &server.Client{Conn: cw})
			}
		}
	}
}

func FileHandler(context *server.Context) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		if stream, ok := vars["stream"]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata, ok := context.Streams[stream]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata.ContainerInit == nil {
			http.Error(w, "Try again a bit later", http.StatusPartialContent)
			return
		} else {
			w.Header().Set("Content-Type", "video/mp4")
			sdata.Logger.Println("file:// client connected", r.RemoteAddr)
			cw := &HttpWriter{w, make(chan struct{}), make(chan []byte, 4), nil}
			go func() {
				for {
					msg, ok := <-cw.Chan
					if !ok {
						return
					}
					if cw.Err != nil {
						return
					}
					_, cw.Err = cw.Conn.Write(msg)
					cw.Conn.(http.Flusher).Flush()
				}
			}()

			cw.Write(sdata.ContainerInit)
			sdata.Clients = append(sdata.Clients, &server.Client{Conn: cw})
			<-cw.Closer
			sdata.Logger.Println("file:// client disconnected", r.RemoteAddr)
		}
	}
}
*/

func MakeClient(conn net.Conn, name string) *server.HttpClient {
	client := &server.HttpClient{
		Queue:  make(chan *server.Payload, 60),
		Signal: make(chan bool),
		Open:   true,
		Logger: log.New(os.Stdout, "HTTP client "+conn.RemoteAddr().String()+" ["+name+"] ", log.LstdFlags),
		Conn:   conn,
	}

	go func() {
		drain := make([]byte, 1024)
		for {
			if _, err := client.Conn.Read(drain); err != nil {
				client.Close()
				return
			}
		}
	}()

	return client
}

func ChunkedClient(client *server.HttpClient) {
	for {
		select {
		case msg := <-client.Queue:
			l := strconv.FormatUint(uint64(len(msg.Data)), 16)
			if _, err := client.Conn.Write([]byte(l)); err != nil {
				return
			}
			if _, err := client.Conn.Write([]byte{'\r', '\n'}); err != nil {
				return
			}
			if _, err := client.Conn.Write(msg.Data); err != nil {
				return
			}
			if _, err := client.Conn.Write([]byte{'\r', '\n'}); err != nil {
				return
			}
		case <-client.Signal:
			return
		}
	}
}

func WebsocketClient(client *server.HttpClient) {
	fmt.Println(123)
	for {
		select {
		case msg := <-client.Queue:
			if _, err := client.Conn.Write(msg.Data); err != nil {
				var header []byte
				if len(msg.Data) < 126 {
					header = []byte{1<<7 | 2, byte(len(msg.Data))}
				} else if len(msg.Data) < 65536 {
					header = []byte{1<<7 | 2, 126, byte(len(msg.Data) << 8), byte(len(msg.Data))}
				} else {
					header = []byte{1<<7 | 2, 127,
						byte(len(msg.Data) << 56),
						byte(len(msg.Data) << 48),
						byte(len(msg.Data) << 40),
						byte(len(msg.Data) << 32),
						byte(len(msg.Data) << 24),
						byte(len(msg.Data) << 16),
						byte(len(msg.Data) << 8),
						byte(len(msg.Data)),
					}
				}

				client.Conn.Write(header)
				client.Conn.Write(msg.Data)
				return
			}
		case <-client.Signal:
			return
		}
	}
}

func ScanHeader(conn net.Conn, header string) (string, error) {
	bbuf := make([]byte, 1)
	value := make([]byte, 0)
	binary := 0
	ptr := 0
	for {
		if _, err := conn.Read(bbuf); err != nil {
			return "", err
		}
		if bbuf[0] == '\r' || bbuf[0] == '\n' {
			binary++
		} else {
			binary = 0
		}

		if binary == 4 {
			return "", NotFound
		} else if binary == 2 {
			ptr = 0
		} else if binary == 0 {
			if ptr < 0 {
				continue
			} else {
				if ptr == len(header) && bbuf[0] == ':' {
					for {
						if _, err := conn.Read(bbuf); err != nil {
							return "", err
						}
						if bbuf[0] != ' ' {
							value = append(value, bbuf[0])
							break
						}
					}
					break
				}
				if bbuf[0] == header[ptr] {
					ptr++
				} else {
					ptr = -1
				}
			}
		}
	}

	for {
		if _, err := conn.Read(bbuf); err != nil {
			return "", err
		}
		if bbuf[0] == '\r' || bbuf[0] == '\n' {
			return string(value), nil
		}
		value = append(value, bbuf[0])
	}
}

func Serve(context *server.Context, conn net.Conn) {
	mbuf := make([]byte, 3)
	if _, err := io.ReadFull(conn, mbuf); err != nil {
		return
	}

	if !bytes.Equal(mbuf, []byte{'G', 'E', 'T'}) {
		conn.Write([]byte("HTTP/1.1 400 Bad request\r\nContent-Length: 0\r\n\r\n"))
		return
	}

	bbuf := make([]byte, 1)
	path := make([]byte, 0)
	for {
		if _, err := conn.Read(bbuf); err != nil {
			return
		}
		if bbuf[0] != ' ' {
			path = append(path, bbuf[0])
			break
		}
	}

	for {
		if _, err := conn.Read(bbuf); err != nil {
			return
		}
		if bbuf[0] == ' ' {
			break
		}
		path = append(path, bbuf[0])
	}

	if len(path) <= len(prefix) || !bytes.Equal(path[:len(prefix)], []byte(prefix)) {
		conn.Write([]byte("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"))
		return
	}

	var client *server.HttpClient
	var stream *server.Stream
	if bytes.Equal(path[len(path)-len(fileSuffix):], []byte(fileSuffix)) {
		name := string(path[len(prefix) : len(path)-len(fileSuffix)])
		if res, ok := context.Streams[name]; !ok {
			conn.Write([]byte("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"))
			return
		} else {
			client.Conn.Write([]byte("HTTP/1.1 200 Ok\r\nContent-Type: video/mp4\r\nTransfer-Encoding: chunked\r\n\r\n"))

			stream = res
			client = MakeClient(conn, name)
			client.Logger.Println("Connected")
			stream.Inclients <- client
			ChunkedClient(client)
		}
	} else if bytes.Equal(path[len(path)-len(wssSuffix):], []byte(wssSuffix)) {
		if header, err := ScanHeader(conn, websockHeader); err != nil {
			conn.Write([]byte("HTTP/1.1 500 Internal error\r\nContent-Length: 0\r\n\r\n"))
			return
		} else {
			name := string(path[len(prefix) : len(path)-len(wssSuffix)])
			if res, ok := context.Streams[name]; !ok {
				conn.Write([]byte("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"))
				return
			} else {
				concat := header + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
				sum := sha1.Sum([]byte(concat))
				accept := base64.StdEncoding.EncodeToString(sum[:])
				conn.Write([]byte("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n"))
				stream = res
				client = MakeClient(conn, name)
				client.Logger.Println("Connected")
				stream.Inclients <- client
				WebsocketClient(client)
			}
		}
	}

	client.Open = false
	stream.Outclients <- client
	close(client.Queue)
	close(client.Signal)
}

func Server(listen string, context *server.Context) error {
	if listener, err := net.Listen("tcp", listen); err != nil {
		return err
	} else {
		for {
			if conn, err := listener.Accept(); err != nil {
				listener.Close()
				return err
			} else {
				go func() {
					Serve(context, conn)
					conn.Close()
				}()
			}
		}
		return listener.Close()
	}
}
