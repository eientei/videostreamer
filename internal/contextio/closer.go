package contextio

import (
	"context"
	"io"
)

type closer struct {
	ctx context.Context
	c   io.Closer
}

func (closer *closer) Close() (err error) {
	if err = closer.c.Close(); err != nil {
		return
	}

	err = closer.ctx.Err()

	return
}
