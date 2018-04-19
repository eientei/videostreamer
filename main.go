package main

import (
	"context"
	"github.com/eientei/videostreamer/server"
)

func main() {
	server := server.NewServer(1935, 8080)
	ctx, cancel := context.WithCancel(context.Background())
	server.Serve(ctx)
	server.Wait()
	cancel()
}
