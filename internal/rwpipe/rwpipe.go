package rwpipe

import (
	"bytes"
	"io"
	"sync"
)

// RWPipe creates two ReadWriteClosers that are connected to each other
// Bytes written to one would be stored in internal buffer and available for reading from another
type RWPipe struct {
	Buf    bytes.Buffer
	Mutex  sync.Mutex
	Pipe   *RWPipe
	closed bool
}

func (rw *RWPipe) Read(p []byte) (n int, err error) {
	rw.Mutex.Lock()
	defer rw.Mutex.Unlock()

	if rw.closed {
		return 0, io.EOF
	}

	if rw.Buf.Len() < len(p) {
		p = p[:rw.Buf.Len()]
	}

	n, err = rw.Buf.Read(p)

	return
}

func (rw *RWPipe) Write(p []byte) (n int, err error) {
	rw.Pipe.Mutex.Lock()
	defer rw.Pipe.Mutex.Unlock()

	if rw.closed {
		return 0, io.EOF
	}

	n, err = rw.Pipe.Buf.Write(p)

	return
}

// Close closes both pipes
func (rw *RWPipe) Close() error {
	rw.Mutex.Lock()

	if rw.closed {
		rw.Mutex.Unlock()
		return nil
	}

	rw.closed = true

	rw.Buf.Reset()
	rw.Mutex.Unlock()

	if !rw.Pipe.closed {
		rw.Pipe.Mutex.Lock()

		rw.Pipe.closed = true

		rw.Buf.Reset()
		rw.Pipe.Mutex.Unlock()
	}

	return nil
}

// Closed returns true if pipes are closed
func (rw *RWPipe) Closed() bool {
	rw.Mutex.Lock()
	defer rw.Mutex.Unlock()

	return rw.closed
}
