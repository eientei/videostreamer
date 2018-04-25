package util

import "io"

type ExpectBuf struct {
	Expect []byte
	ptr    int
}

func (buf *ExpectBuf) Write(data []byte) (int, error) {
	for i, b := range data {
		if buf.ptr >= len(buf.Expect) {
			return i, io.EOF
		}
		if buf.Expect[buf.ptr] != b {
			return i, io.ErrUnexpectedEOF
		}
		buf.ptr++
	}
	return len(data), nil
}

func (buf *ExpectBuf) Finalize() error {
	if buf.ptr != len(buf.Expect) {
		buf.ptr = 0
		return io.ErrUnexpectedEOF
	}
	buf.ptr = 0
	return nil
}
