package amf

import (
	"bytes"
	"testing"

	"../util"
)

func TestBooleanValue_Type(t *testing.T) {
	if (&BooleanValue{}).Type() != Boolean {
		t.Fatal()
	}
}

func TestBooleanValue_Read(t *testing.T) {
	value := &BooleanValue{}

	if err := value.Read(bytes.NewReader([]byte{0x00})); err != nil {
		t.Fatal(err)
	}

	if value.Data {
		t.Fatal()
	}

	if err := value.Read(bytes.NewReader([]byte{0x01})); err != nil {
		t.Fatal(err)
	}

	if !value.Data {
		t.Fatal()
	}

	if err := value.Read(bytes.NewReader([]byte{})); err == nil {
		t.Fatal()
	}
}

func TestBooleanValue_Write(t *testing.T) {
	value := &BooleanValue{false}

	wfalse := &util.ExpectBuf{Expect: []byte{0x00}}
	if err := value.Write(wfalse); err != nil {
		t.Fatal(err)
	}
	if err := wfalse.Finalize(); err != nil {
		t.Fatal(err)
	}

	value.Data = true

	wtrue := &util.ExpectBuf{Expect: []byte{0x01}}
	if err := value.Write(wtrue); err != nil {
		t.Fatal(err)
	}
	if err := wtrue.Finalize(); err != nil {
		t.Fatal(err)
	}
}

func TestBooleanValue_String(t *testing.T) {
	value := &BooleanValue{false}

	if value.String() != "false" {
		t.Fatal()
	}

	value.Data = true

	if value.String() != "true" {
		t.Fatal()
	}
}
