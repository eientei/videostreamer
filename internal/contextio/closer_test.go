package contextio

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type testCloser struct {
}

func (t *testCloser) Close() error {
	return nil
}

func TestCloser_Close(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := &closer{
		ctx: ctx,
		c:   &testCloser{},
	}
	err := impl.Close()
	assert.NoError(t, err)
	cancel()
	err = impl.Close()
	assert.EqualError(t, err, "context canceled")
}
