package rwpipe

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNew(t *testing.T) {
	a, b := New()
	n, err := a.Write([]byte{'a', 'b', 'c'})
	assert.NoError(t, err)
	assert.EqualValues(t, 3, n)
	bs := make([]byte, 10)
	n, err = b.Read(bs)
	assert.NoError(t, err)
	assert.EqualValues(t, 3, n)
	assert.EqualValues(t, []byte{'a', 'b', 'c'}, bs[:3])

	n, err = b.Write([]byte{'e', 'f', 'g'})
	assert.NoError(t, err)
	assert.EqualValues(t, 3, n)
	n, err = a.Read(bs)
	assert.NoError(t, err)
	assert.EqualValues(t, 3, n)
	assert.EqualValues(t, []byte{'e', 'f', 'g'}, bs[:3])

	n, err = a.Write([]byte{'e', 'f', 'g'})
	assert.NoError(t, err)
	assert.EqualValues(t, 3, n)

	err = a.Close()
	assert.NoError(t, err)
	assert.True(t, a.Closed())
	assert.True(t, b.Closed())

	n, err = a.Write([]byte{'g', 'f', 'h'})
	assert.Error(t, err)
	assert.EqualValues(t, 0, n)

	err = b.Close()
	assert.NoError(t, err)

	n, err = b.Write([]byte{'g', 'f', 'h'})
	assert.Error(t, err)
	assert.EqualValues(t, 0, n)

	n, err = b.Read(bs)
	assert.Error(t, err)
	assert.EqualValues(t, 0, n)
}
