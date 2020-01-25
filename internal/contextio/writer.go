package contextio

import (
	"context"
	"io"
	"time"
)

type writeDeadliner interface {
	SetWriteDeadline(time.Time) error
}

func writerDeadline(ctx context.Context, w io.Writer) {
	if deadline, ok := ctx.Deadline(); ok {
		if d, ok := w.(writeDeadliner); ok {
			_ = d.SetWriteDeadline(deadline)
		}
	}
}

func (writer writer) Write(p []byte) (n int, err error) {
	if err = writer.ctx.Err(); err != nil {
		return
	}

	if n, err = writer.w.Write(p); err != nil {
		return
	}

	err = writer.ctx.Err()

	return
}
