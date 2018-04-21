package amf

import (
	"bytes"
	"math"
	"testing"

	"github.com/eientei/videostreamer/util"
)

func TestNumberValue_Type(t *testing.T) {
	if (&NumberValue{}).Type() != Number {
		t.Fatal()
	}
}

func TestNumberValue_Read(t *testing.T) {
	buf := make([]byte, 8)
	util.WriteB64(buf, math.Float64bits(123.456))

	value := &NumberValue{}

	if err := value.Read(bytes.NewReader(buf)); err != nil {
		t.Fatal(err)
	}

	if value.Data != 123.456 {
		t.Fatal(value.Data)
	}

	if err := value.Read(bytes.NewReader(buf[:0])); err == nil {
		t.Fatal()
	}

	if err := value.Read(bytes.NewReader(buf[:4])); err == nil {
		t.Fatal()
	}
}

func TestNumberValue_Write(t *testing.T) {
	buf := make([]byte, 8)
	util.WriteB64(buf, math.Float64bits(123.456))

	value := &NumberValue{123.456}

	w123456 := &util.ExpectBuf{Expect: buf}
	if err := value.Write(w123456); err != nil {
		t.Fatal(err)
	}
	if err := w123456.Finalize(); err != nil {
		t.Fatal(err)
	}
}

func TestNumberValue_String(t *testing.T) {
	if (&NumberValue{123.456}).String() != "123.456" {
		t.Fatal()
	}
}
