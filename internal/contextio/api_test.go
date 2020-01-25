package contextio

import (
	"context"
	"io"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestReader(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := Reader(ctx, &testReader{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
}

func TestWriter(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := Writer(ctx, &testWriter{})
	_, err := impl.Write(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")
}

type testReadCloser struct {
	testReader
	testCloser
}

func TestReadCloser(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := ReadCloser(ctx, &testReadCloser{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")

	ctx, cancel = context.WithTimeout(context.Background(), time.Second)
	impl = ReadCloser(ctx, &testReadCloser{})
	err = impl.Close()
	assert.NoError(t, err)
	cancel()
	err = impl.Close()
	assert.EqualError(t, err, "context canceled")
}

type testWriteCloser struct {
	testWriter
	testCloser
}

func TestWriteCloser(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := WriteCloser(ctx, &testWriteCloser{})
	_, err := impl.Write(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")

	ctx, cancel = context.WithTimeout(context.Background(), time.Second)
	impl = WriteCloser(ctx, &testWriteCloser{})
	err = impl.Close()
	assert.NoError(t, err)
	cancel()
	err = impl.Close()
	assert.EqualError(t, err, "context canceled")
}

type testReadSeeker struct {
	testReader
	testSeeker
}

func TestReadSeeker(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := ReadSeeker(ctx, &testReadSeeker{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	_, err = impl.Seek(0, io.SeekStart)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Seek(0, io.SeekStart)
	assert.EqualError(t, err, "context canceled")
}

type testWriteSeeker struct {
	testWriter
	testSeeker
}

func TestWriteSeeker(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := WriteSeeker(ctx, &testWriteSeeker{})
	_, err := impl.Write(nil)
	assert.NoError(t, err)
	_, err = impl.Seek(0, io.SeekStart)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Seek(0, io.SeekStart)
	assert.EqualError(t, err, "context canceled")
}

type testReadWriter struct {
	testReader
	testWriter
}

func TestReadWriter(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := ReadWriter(ctx, &testReadWriter{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	_, err = impl.Write(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")
}

type testReadWriteCloser struct {
	testReader
	testWriter
	testCloser
}

func TestReadWriteCloser(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := ReadWriteCloser(ctx, &testReadWriteCloser{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	_, err = impl.Write(nil)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")

	ctx, cancel = context.WithTimeout(context.Background(), time.Second)
	impl = ReadWriteCloser(ctx, &testReadWriteCloser{})
	err = impl.Close()
	assert.NoError(t, err)
	cancel()
	err = impl.Close()
	assert.EqualError(t, err, "context canceled")
}

type testReadWriteSeeker struct {
	testReader
	testWriter
	testSeeker
}

func TestReadWriteSeeker(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	impl := ReadWriteSeeker(ctx, &testReadWriteSeeker{})
	_, err := impl.Read(nil)
	assert.NoError(t, err)
	_, err = impl.Write(nil)
	assert.NoError(t, err)
	_, err = impl.Seek(0, io.SeekStart)
	assert.NoError(t, err)
	cancel()
	_, err = impl.Read(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Write(nil)
	assert.EqualError(t, err, "context canceled")
	_, err = impl.Seek(0, io.SeekStart)
	assert.EqualError(t, err, "context canceled")
}
