package handshake

import (
	"bytes"
	"sync"
)

type rwpipe struct {
	buf   bytes.Buffer
	mutex sync.Mutex
	pipe  *rwpipe
}

func (rw *rwpipe) Read(p []byte) (n int, err error) {
	rw.mutex.Lock()
	defer rw.mutex.Unlock()
	if rw.buf.Len() < len(p) {
		p = p[:rw.buf.Len()]
	}
	n, err = rw.buf.Read(p)
	return
}

func (rw *rwpipe) Write(p []byte) (n int, err error) {
	rw.pipe.mutex.Lock()
	defer rw.pipe.mutex.Unlock()
	n, err = rw.pipe.buf.Write(p)
	return
}

func newRWPipe() (a *rwpipe, b *rwpipe) {
	a = &rwpipe{}
	b = &rwpipe{}
	a.pipe, b.pipe = b, a
	return
}
