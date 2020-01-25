package contextio

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type testReader struct {
}

func (t *testReader) Read(b []byte) (n int, err error) {
	return 0, nil
}

func (t *testReader) SetReadDeadline(d time.Time) error {
	return nil
}

func TestReader_Read(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := &reader{
		ctx: ctx,
		r:   &testReader{},
	}
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
}
