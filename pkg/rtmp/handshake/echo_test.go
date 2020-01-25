package handshake

import (
	"sync"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestEcho_Default(t *testing.T) {
	c := NewEchoHandshake()
	s := NewEchoHandshake()

	crw, srw := newRWPipe()

	wg := &sync.WaitGroup{}
	wg.Add(2)

	go func() {
		_, pt, err := c.Handshake(crw)
		assert.NoError(t, err)
		assert.EqualValues(t, 0, pt)
		wg.Done()
	}()

	go func() {
		_, pt, err := s.Handshake(srw)
		assert.NoError(t, err)
		assert.EqualValues(t, 0, pt)
		wg.Done()
	}()
	wg.Wait()
}
