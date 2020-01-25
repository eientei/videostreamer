package main

import (
	"context"
	"fmt"
	"net"

	"github.com/eientei/videostreamer/pkg/amf0"
	"github.com/eientei/videostreamer/pkg/rtmp"
)

func main() {
	listener, err := net.Listen("tcp", ":1935")
	if err != nil {
		panic(err)
	}

	server := rtmp.NewServer(context.Background(), listener, nil)

	for {
		conn, err := server.Accept(context.Background())
		if err != nil {
			panic(err)
		}

		go func() {
			for {
				msg, err := conn.Recv(context.Background())
				if err != nil {
					fmt.Println(err)
					return
				}

				fmt.Println(amf0.UnmarshalAllBytes(msg.Data))
			}
		}()
	}
}
