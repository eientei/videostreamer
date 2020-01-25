package contextio

import (
	"context"
	"io"
)

type seeker struct {
	ctx context.Context
	s   io.Seeker
}

func (seeker *seeker) Seek(offset int64, whence int) (n int64, err error) {
	if err = seeker.ctx.Err(); err != nil {
		return
	}

	if n, err = seeker.s.Seek(offset, whence); err != nil {
		return
	}

	err = seeker.ctx.Err()

	return
}
