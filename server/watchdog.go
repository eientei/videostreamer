package server

import (
	"net"
)

type WatchState int

const (
	WatchEnd = WatchState(iota)
	ConnOpen = WatchState(iota)
	ConnClose = WatchState(iota)
)

type WatchEvent struct {
	Conn net.Conn
	State WatchState
}

func Watchdog(done chan struct{}, watch chan WatchEvent) {
	conns := make([]net.Conn, 0)
	watchEnd := false
	loop: for {
		event := <- watch
		switch event.State {
		case WatchEnd:
			if len(conns) == 0 {
				break loop
			}
			watchEnd = true
		case ConnOpen:
			conns = append(conns, event.Conn)
		case ConnClose:
			for i, conn := range conns {
				if conn == event.Conn {
					conns = append(conns[:i], conns[i+1:]...)
					break
				}
			}
			if watchEnd && len(conns) == 0 {
				break loop
			}
		}
	}
	close(watch)
	close(done)
}