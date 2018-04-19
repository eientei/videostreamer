package server

import (
	"testing"
	"context"
	"net"
)

func TestNewServer(t *testing.T) {
	server1 := NewServer(0, 0)
	if server1.RtmpPort != 0 {
		t.Fatal()
	}
	if server1.HttpPort != 0 {
		t.Fatal()
	}

	server2 := NewServer(12, 34)
	if server2.RtmpPort != 12 {
		t.Fatal()
	}
	if server2.HttpPort != 34 {
		t.Fatal()
	}
}

func TestServer_Serve(t *testing.T) {
	server := NewServer(1935, 8060)
	ctx, cancel := context.WithCancel(context.Background())
	if err := server.Serve(ctx); err != nil {
		t.Fatal(err)
	}
	if conn, err := net.Dial("tcp", ":1935"); err != nil {
		t.Fatal(err)
	} else {
		conn.Close()
	}

	if conn, err := net.Dial("tcp", ":8060"); err != nil {
		t.Fatal(err)
	} else {
		conn.Close()
	}

	cancel()
	server.Wait()

	if conn, err := net.Dial("tcp", ":1935"); err == nil {
		conn.Close()
		t.Fatal(err)
	}

	if conn, err := net.Dial("tcp", ":8060"); err == nil {
		conn.Close()
		t.Fatal(err)
	}
}

func TestServer_ServeRtmpFail(t *testing.T) {
	server := NewServer(1, 8060)
	ctx, cancel := context.WithCancel(context.Background())
	if err := server.Serve(ctx); err == nil {
		cancel()
		server.Wait()
		t.Fatal(err)
	}
}

func TestServer_ServeHttpFail(t *testing.T) {
	server := NewServer(1935, 1)
	ctx, cancel := context.WithCancel(context.Background())
	if err := server.Serve(ctx); err == nil {
		cancel()
		server.Wait()
		t.Fatal(err)
	}
}