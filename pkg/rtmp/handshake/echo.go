package handshake

import (
	"io"
	"math/rand"
	"time"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// NewEchoHandshake returns new simple echo handshaker instance
func NewEchoHandshake() Handshaker {
	return &echo{}
}

type echo struct {
}

func (impl *echo) Handshake(rw io.ReadWriter) (timestamp time.Time, peerDelta uint32, err error) {
	timestamp = time.Now()
	buf := make([]byte, 1537)
	l0 := buf[:1]
	l0[0] = 0x03
	l1 := buf[1:]
	l1time := l1[:4]
	l1data := l1[8:]

	byteorder.BigEndian.PutUint32(l1time, 0)
	rand.Seed(time.Now().UnixNano())

	_, err = rand.Read(l1data)
	if err != nil {
		return
	}

	_, err = rw.Write(buf)
	if err != nil {
		return
	}

	_, err = io.ReadFull(rw, buf)
	if err != nil {
		return
	}

	now := time.Now()
	r1 := buf[1:]
	r1time := r1[:4]
	peerDelta = byteorder.BigEndian.Uint32(r1time)

	r2 := buf[1:]
	r2peerTime := r2[:4]
	r2recvTime := r2[4:8]

	byteorder.BigEndian.PutUint32(r2peerTime, peerDelta)
	byteorder.BigEndian.PutUint32(r2recvTime, uint32(now.Sub(timestamp).Milliseconds()))

	_, err = rw.Write(r2)
	if err != nil {
		return
	}

	_, err = io.ReadFull(rw, l1)
	if err != nil {
		return
	}

	return
}
