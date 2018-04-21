package util

import (
	"bytes"
	"testing"
)

func TestReadB16(t *testing.T) {
	if ReadB16([]byte{0x12, 0x34}) != 0x1234 {
		t.Fatal()
	}
}

func TestReadB24(t *testing.T) {
	if ReadB24([]byte{0x12, 0x34, 0x56}) != 0x123456 {
		t.Fatal()
	}
}

func TestReadB32(t *testing.T) {
	if ReadB32([]byte{0x12, 0x34, 0x56, 0x78}) != 0x12345678 {
		t.Fatal()
	}
}

func TestReadB64(t *testing.T) {
	if ReadB64([]byte{0x12, 0x34, 0x56, 0x78, 0x90, 0xab, 0xcd, 0xef}) != 0x1234567890abcdef {
		t.Fatal()
	}
}

func TestReadL16(t *testing.T) {
	if ReadL16([]byte{0x34, 0x12}) != 0x1234 {
		t.Fatal()
	}
}

func TestReadL24(t *testing.T) {
	if ReadL24([]byte{0x56, 0x34, 0x12}) != 0x123456 {
		t.Fatal()
	}
}

func TestReadL32(t *testing.T) {
	if ReadL32([]byte{0x78, 0x56, 0x34, 0x12}) != 0x12345678 {
		t.Fatal()
	}
}

func TestReadL64(t *testing.T) {
	if ReadL64([]byte{0xef, 0xcd, 0xab, 0x90, 0x78, 0x56, 0x34, 0x12}) != 0x1234567890abcdef {
		t.Fatal()
	}
}

func TestWriteB16(t *testing.T) {
	buf := make([]byte, 2)
	WriteB16(buf, 0x1234)
	if !bytes.Equal(buf, []byte{0x12, 0x34}) {
		t.Fatal()
	}
}

func TestWriteB24(t *testing.T) {
	buf := make([]byte, 3)
	WriteB24(buf, 0x123456)
	if !bytes.Equal(buf, []byte{0x12, 0x34, 0x56}) {
		t.Fatal()
	}
}

func TestWriteB32(t *testing.T) {
	buf := make([]byte, 4)
	WriteB32(buf, 0x12345678)
	if !bytes.Equal(buf, []byte{0x12, 0x34, 0x56, 0x78}) {
		t.Fatal()
	}
}

func TestWriteB64(t *testing.T) {
	buf := make([]byte, 8)
	WriteB64(buf, 0x1234567890abcdef)
	if !bytes.Equal(buf, []byte{0x12, 0x34, 0x56, 0x78, 0x90, 0xab, 0xcd, 0xef}) {
		t.Fatal()
	}
}

func TestWriteL16(t *testing.T) {
	buf := make([]byte, 2)
	WriteL16(buf, 0x1234)
	if !bytes.Equal(buf, []byte{0x34, 0x12}) {
		t.Fatal()
	}
}

func TestWriteL24(t *testing.T) {
	buf := make([]byte, 3)
	WriteL24(buf, 0x123456)
	if !bytes.Equal(buf, []byte{0x56, 0x34, 0x12}) {
		t.Fatal()
	}
}

func TestWriteL32(t *testing.T) {
	buf := make([]byte, 4)
	WriteL32(buf, 0x12345678)
	if !bytes.Equal(buf, []byte{0x78, 0x56, 0x34, 0x12}) {
		t.Fatal()
	}
}

func TestWriteL64(t *testing.T) {
	buf := make([]byte, 8)
	WriteL64(buf, 0x1234567890abcdef)
	if !bytes.Equal(buf, []byte{0xef, 0xcd, 0xab, 0x90, 0x78, 0x56, 0x34, 0x12}) {
		t.Fatal()
	}
}
