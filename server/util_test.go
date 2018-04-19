package server

import (
	"testing"
	"bytes"
)

func TestBUint32(t *testing.T) {
	if BUint32([]byte{0x00, 0x00, 0x00, 0x01}) != 1 {
		t.Fatal()
	}
}

func TestBUint24(t *testing.T) {
	if BUint24([]byte{0x00, 0x00, 0x01}) != 1 {
		t.Fatal()
	}
}

func TestLUint24(t *testing.T) {
	if LUint24([]byte{0x01, 0x00, 0x00}) != 1 {
		t.Fatal()
	}
}

func TestPutBUint32(t *testing.T) {
	buf := make([]byte, 4)
	PutBUint32(buf, 1)
	if !bytes.Equal(buf, []byte{0x00, 0x00, 0x00, 0x01}) {
		t.Fatal()
	}
}

func TestPutBUint24(t *testing.T) {
	buf := make([]byte, 3)
	PutBUint24(buf, 1)
	if !bytes.Equal(buf, []byte{0x00, 0x00, 0x01}) {
		t.Fatal()
	}
}

func TestPutLUint24(t *testing.T) {
	buf := make([]byte, 3)
	PutLUint24(buf, 1)
	if !bytes.Equal(buf, []byte{0x01, 0x00, 0x00}) {
		t.Fatal()
	}
}