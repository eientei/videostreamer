package handshake

import (
	"sync"
	"testing"

	"github.com/eientei/videostreamer/internal/rwpipe"
	"github.com/stretchr/testify/assert"
)

func TestKeys_Alt(t *testing.T) {
	c := NewClientKeysHandshake(&KeysConfig{
		Algorithm: 1,
	})
	s := NewServerKeysHandshake(&KeysConfig{
		Algorithm: 0,
	})
	crw, srw := rwpipe.New()

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

func TestKeys_Default(t *testing.T) {
	c := NewClientKeysHandshake(nil)
	s := NewServerKeysHandshake(nil)

	crw, srw := rwpipe.New()

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
