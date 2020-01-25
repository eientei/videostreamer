package contextio

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type testWriter struct {
}

func (t *testWriter) Write(b []byte) (n int, err error) {
	return 0, nil
}

func (t *testWriter) SetWriteDeadline(d time.Time) error {
	return nil
}

func TestWriter_Write(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := &writer{
		ctx: ctx,
		w:   &testWriter{},
	}
	_, err := impl.Write(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")
}
