package amf

import (
	"bytes"
	"testing"

	"../util"
)

func TestStringValue_Type(t *testing.T) {
	if (&StringValue{}).Type() != String {
		t.Fatal()
	}
}

func TestStringValue_Read(t *testing.T) {
	value := &StringValue{}

	if err := value.Read(bytes.NewReader([]byte{0x00, 0x03, 'a', 'b', 'c'})); err != nil {
		t.Fatal(err)
	}

	if value.Data != "abc" {
		t.Fatal()
	}

	if err := value.Read(bytes.NewReader([]byte{0x00, 0x03})); err == nil {
		t.Fatal()
	}

	if err := value.Read(bytes.NewReader([]byte{})); err == nil {
		t.Fatal()
	}
}

func TestStringValue_Write(t *testing.T) {
	value := &StringValue{"abc"}

	wstr := &util.ExpectBuf{Expect: []byte{0x00, 0x03, 'a', 'b', 'c'}}

	if err := value.Write(wstr); err != nil {
		t.Fatal(err)
	}

	if err := wstr.Finalize(); err != nil {
		t.Fatal(err)
	}

	if err := value.Write(&util.ExpectBuf{Expect: []byte{}}); err == nil {
		t.Fatal()
	}

	if err := value.Write(&util.ExpectBuf{Expect: []byte{0x00, 0x03}}); err == nil {
		t.Fatal()
	}
}

func TestStringValue_String(t *testing.T) {
	value := &StringValue{"abc"}
	if value.String() != "\"abc\"" {
		t.Fatal()
	}
}
