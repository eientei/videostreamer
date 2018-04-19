package server

import (
	"net/http"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"fmt"
	"bytes"
	"time"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type ConnWriter struct {
	Conn *websocket.Conn
}

func (cw *ConnWriter) Write(data []byte) (int, error) {
	err := cw.Conn.WriteMessage(websocket.BinaryMessage, data)
	return len(data), err
}

func (cw *ConnWriter) Close() error {
	return cw.Conn.Close()
}

type HttpWriter struct {
	Conn http.ResponseWriter
	Closer chan struct{}
}
func (cw *HttpWriter) Write(data []byte) (int, error) {
	n, err := cw.Conn.Write(data)
	cw.Conn.(http.Flusher).Flush()
	return n, err
}

func (cw *HttpWriter) Close() error {
	close(cw.Closer)
	return nil
}

func VideoHandler(server *Server) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		if stream, ok := vars["stream"]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata, ok := server.Streams[stream]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata.Moov == nil {
			http.Error(w, "Try again a bit later", http.StatusPartialContent)
			return
		} else {
			if conn, err := upgrader.Upgrade(w, r, nil); err != nil {
				fmt.Println(err)
				http.Error(w, err.Error(), http.StatusInternalServerError)
			} else {
				conn.SetReadDeadline(time.Time{})
				conn.SetCloseHandler(func(code int, text string) error {
					fmt.Println(code, text)
					return nil
				})
				sdata.Clients = append(sdata.Clients, &Client{&ConnWriter{conn}, false, 0, 0, 0, &bytes.Buffer{}})
			}
		}
	}
}

func FileHandler(server *Server) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		if stream, ok := vars["stream"]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata, ok := server.Streams[stream]; !ok {
			http.NotFound(w, r)
			return
		} else if sdata.Moov == nil {
			http.Error(w, "Try again a bit later", http.StatusPartialContent)
			return
		} else {
			w.Header().Set("X-Content-Type-Options", "nosniff")
			cw := &HttpWriter{w, make(chan struct{})}
			sdata.Clients = append(sdata.Clients, &Client{cw, false, 0, 0, 0,&bytes.Buffer{}})
			<-cw.Closer
		}
	}
}

func HttpServer(addr string, server *Server) {
	r := mux.NewRouter()
	r.HandleFunc("/video/{stream}", VideoHandler(server))
	r.HandleFunc("/file/{stream}.mp4", FileHandler(server))
	r.PathPrefix("/").Handler(http.FileServer(http.Dir("./static")))
	s := &http.Server{
		Addr: addr,
		Handler: r,
	}
	s.ListenAndServe()
	close(server.HttpDone)
}
