package contextio

import (
	"context"
	"io"
	"time"
)

type readDeadliner interface {
	SetReadDeadline(time.Time) error
}

func readerDeadline(ctx context.Context, r io.Reader) {
	if deadline, ok := ctx.Deadline(); ok {
		if d, ok := r.(readDeadliner); ok {
			_ = d.SetReadDeadline(deadline)
		}
	}
}

func (reader *reader) Read(p []byte) (n int, err error) {
	if err = reader.ctx.Err(); err != nil {
		return
	}

	if n, err = reader.r.Read(p); err != nil {
		return
	}

	err = reader.ctx.Err()

	return
}
