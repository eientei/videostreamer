package http

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"strconv"

	"../server"
)

const prefix = "/live/"
const suffix = ".mp4"

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

	if len(path) <= len(prefix)+len(suffix) || !bytes.Equal(path[:len(prefix)], []byte(prefix)) || !bytes.Equal(path[len(path)-len(suffix):], []byte(suffix)) {
		conn.Write([]byte("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"))
		return
	}

	name := string(path[len(prefix) : len(path)-len(suffix)])

	var stream *server.Stream
	if res, ok := context.Streams[name]; !ok {
		conn.Write([]byte("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"))
		return
	} else {
		stream = res
	}

	logger := log.New(os.Stdout, "HTTP client "+conn.RemoteAddr().String()+" ["+name+"] ", log.LstdFlags)
	logger.Println("Connected")

	conn.Write([]byte("HTTP/1.1 200 Ok\r\nContent-Type: video/mp4\r\nTransfer-Encoding: chunked\r\n\r\n"))

	client := &server.HttpClient{
		Queue:  make(chan *server.Payload),
		Signal: make(chan bool),
		Open:   true,
	}

	go func() {
		drain := make([]byte, 1024)
		for {
			if _, err := conn.Read(drain); err != nil {
				client.Close()
				return
			}
		}
	}()

	stream.Inclients <- client
loop:
	for {
		select {
		case msg := <-client.Queue:
			fmt.Println(len(msg.Data))
			l := strconv.FormatUint(uint64(len(msg.Data)), 16)
			if _, err := conn.Write([]byte(l)); err != nil {
				break loop
			}
			if _, err := conn.Write([]byte{'\r', '\n'}); err != nil {
				break loop
			}
			if _, err := conn.Write(msg.Data); err != nil {
				break loop
			}
			if _, err := conn.Write([]byte{'\r', '\n'}); err != nil {
				break loop
			}
		case <-client.Signal:
			break loop
		}
	}
	client.Open = false
	stream.Outclients <- client
	close(client.Queue)
	close(client.Signal)
	logger.Println("Disconnected")
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
