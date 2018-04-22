package http

import (
	"log"
	"net/http"

	"../server"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
)

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
	err := cw.Conn.WriteMessage(websocket.BinaryMessage, data)
	return len(data), err
}

func (cw *ConnWriter) Close() error {
	cw.Logger.Println("wss:// client disconnected", cw.Conn.RemoteAddr().String())
	return cw.Conn.Close()
}

type HttpWriter struct {
	Conn   http.ResponseWriter
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
				cw := &ConnWriter{sdata.Logger, conn}
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
			cw := &HttpWriter{w, make(chan struct{})}
			cw.Write(sdata.ContainerInit)
			sdata.Clients = append(sdata.Clients, &server.Client{Conn: cw})
			<-cw.Closer
			sdata.Logger.Println("file:// client disconnected", r.RemoteAddr)
		}
	}
}

func Server(addr string, context *server.Context) {
	r := mux.NewRouter()
	r.HandleFunc("/live/{stream}.wss", WebsocketHandler(context))
	r.HandleFunc("/live/{stream}.mp4", FileHandler(context))
	r.HandleFunc("/live/{stream}.sig", SignalHandler(context))
	r.PathPrefix("/").Handler(http.FileServer(http.Dir("./static")))
	s := &http.Server{
		Addr:    addr,
		Handler: r,
	}
	s.ListenAndServe()
}
