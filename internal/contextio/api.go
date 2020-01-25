// Package contextio provides context-bound wrappers for io.(Read|Write|ReadWrite)(Seek|Close)er interfaces
package contextio

import (
	"context"
	"io"
)

type reader struct {
	ctx context.Context
	r   io.Reader
}

// Reader wraps reader with given context
func Reader(ctx context.Context, r io.Reader) io.Reader {
	readerDeadline(ctx, r)

	return &reader{
		ctx: ctx,
		r:   r,
	}
}

type readCloser struct {
	reader
	closer
}

// ReadCloser wraps readcloser with given context
func ReadCloser(ctx context.Context, r io.ReadCloser) io.ReadCloser {
	readerDeadline(ctx, r)

	return &readCloser{
		reader: reader{
			ctx: ctx,
			r:   r,
		},
		closer: closer{
			ctx: ctx,
			c:   r,
		},
	}
}

type readSeeker struct {
	reader
	seeker
}

// ReadSeeker wraps readseeker with given context
func ReadSeeker(ctx context.Context, r io.ReadSeeker) io.ReadSeeker {
	readerDeadline(ctx, r)

	return &readSeeker{
		reader: reader{
			ctx: ctx,
			r:   r,
		},
		seeker: seeker{
			ctx: ctx,
			s:   r,
		},
	}
}

type writer struct {
	ctx context.Context
	w   io.Writer
}

// Writer wraps writer with given context
func Writer(ctx context.Context, w io.Writer) io.Writer {
	writerDeadline(ctx, w)

	return &writer{
		ctx: ctx,
		w:   w,
	}
}

type writeCloser struct {
	writer
	closer
}

// WriteCloser wraps writecloser with given context
func WriteCloser(ctx context.Context, w io.WriteCloser) io.WriteCloser {
	writerDeadline(ctx, w)

	return &writeCloser{
		writer: writer{
			ctx: ctx,
			w:   w,
		},
		closer: closer{
			ctx: ctx,
			c:   w,
		},
	}
}

type writeSeeker struct {
	writer
	seeker
}

// WriteSeeker wraps writeseeker with given context
func WriteSeeker(ctx context.Context, w io.WriteSeeker) io.WriteSeeker {
	writerDeadline(ctx, w)

	return &writeSeeker{
		writer: writer{
			ctx: ctx,
			w:   w,
		},
		seeker: seeker{
			ctx: ctx,
			s:   w,
		},
	}
}

type readWriter struct {
	reader
	writer
}

// ReadWriter wraps readwriter with given context
func ReadWriter(ctx context.Context, rw io.ReadWriter) io.ReadWriter {
	readerDeadline(ctx, rw)
	writerDeadline(ctx, rw)

	return &readWriter{
		reader: reader{
			ctx: ctx,
			r:   rw,
		},
		writer: writer{
			ctx: ctx,
			w:   rw,
		},
	}
}

type readWriteCloser struct {
	reader
	writer
	closer
}

// ReadWriteCloser wraps readwritecloser with given context
func ReadWriteCloser(ctx context.Context, rw io.ReadWriteCloser) io.ReadWriteCloser {
	readerDeadline(ctx, rw)
	writerDeadline(ctx, rw)

	return &readWriteCloser{
		reader: reader{
			ctx: ctx,
			r:   rw,
		},
		writer: writer{
			ctx: ctx,
			w:   rw,
		},
		closer: closer{
			ctx: ctx,
			c:   rw,
		},
	}
}

type readWriteSeeker struct {
	reader
	writer
	seeker
}

// ReadWriteSeeker wraps readwriteseeker with given context
func ReadWriteSeeker(ctx context.Context, rw io.ReadWriteSeeker) io.ReadWriteSeeker {
	readerDeadline(ctx, rw)
	writerDeadline(ctx, rw)

	return &readWriteSeeker{
		reader: reader{
			ctx: ctx,
			r:   rw,
		},
		writer: writer{
			ctx: ctx,
			w:   rw,
		},
		seeker: seeker{
			ctx: ctx,
			s:   rw,
		},
	}
}
