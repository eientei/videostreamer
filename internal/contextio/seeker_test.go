package contextio

import (
	"context"
	"io"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type testSeeker struct {
}

func (t *testSeeker) Seek(offset int64, whence int) (n int64, err error) {
	return 0, nil
}

func TestSeeker_Seek(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := &seeker{
		ctx: ctx,
		s:   &testSeeker{},
	}
	_, err := impl.Seek(0, io.SeekStart)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Seek(0, io.SeekStart)
	assert.EqualError(t, err, "context canceled")
}
