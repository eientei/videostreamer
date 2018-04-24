package main

import (
	"runtime"

	"./http"
	"./rtmp"
	"./server"
)

func main() {
	runtime.GOMAXPROCS(32)

	context := &server.Context{
		Streams: make(map[string]*server.Stream),
	}
	wait := make(chan struct{})
	go rtmp.Server(":1935", context)
	go http.Server(":8080", context)
	<-wait
}
