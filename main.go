package main

import (
	"./http"
	"./rtmp"
	"./server"
)

func main() {
	context := &server.Context{
		Streams: make(map[string]*server.Stream),
	}
	wait := make(chan struct{})
	go rtmp.Server(":1935", context)
	go http.Server(":8080", context)
	<-wait
}
