package server

import (
	"testing"
	"net"
)

func TestWatchdog(t *testing.T) {
	watch := make(chan WatchEvent)
	done := make(chan struct{})
	go Watchdog(done, watch)
	watch <- WatchEvent{nil, WatchEnd}
	if _, ok := <-done; ok {
		t.Fatal()
	}
	if _, ok := <-watch; ok {
		t.Fatal()
	}
}

func TestWatchdogAdd(t *testing.T) {
	watch := make(chan WatchEvent)
	done := make(chan struct{})
	go Watchdog(done, watch)
	conn := &net.TCPConn{}
	watch <- WatchEvent{conn, ConnOpen}
	watch <- WatchEvent{conn, ConnClose}
	watch <- WatchEvent{conn, ConnOpen}
	watch <- WatchEvent{nil, WatchEnd}
	watch <- WatchEvent{conn, ConnClose}
	if _, ok := <-done; ok {
		t.Fatal()
	}
	if _, ok := <-watch; ok {
		t.Fatal()
	}
}