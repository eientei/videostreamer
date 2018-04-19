package server

import (
	"testing"
	"math"
	"encoding/binary"
	"bytes"
)

func TestAmfNumber_Id(t *testing.T) {
	n := &AmfNumber{}
	if n.Id() != Number {
		t.Fatal()
	}
}

func TestAmfNumber_Read(t *testing.T) {
	n := &AmfNumber{}
	buf := make([]byte, 8)
	binary.BigEndian.PutUint64(buf, math.Float64bits(3.14))
	n.Read(bytes.NewReader(buf))
	if n.Value != 3.14 {
		t.Fatal()
	}
}

func TestAmfNumber_Write(t *testing.T) {
	n := &AmfNumber{3.14}
	buf := &bytes.Buffer{}
	n.Write(buf)
	ref := make([]byte, 8)
	binary.BigEndian.PutUint64(ref, math.Float64bits(3.14))
	if !bytes.Equal(buf.Bytes(), ref) {
		t.Fatal()
	}
}

func TestAmfNumber_String(t *testing.T) {
	n := &AmfNumber{3.14}
	if n.String() != "3.14" {
		t.Fatal()
	}
}

func TestAmfBoolean_Id(t *testing.T) {
	n := &AmfBoolean{}
	if n.Id() != Boolean {
		t.Fatal()
	}
}

func TestAmfBoolean_Read(t *testing.T) {
	n := &AmfBoolean{}
	buf := []byte{0x01}
	n.Read(bytes.NewReader(buf))
	if n.Value != true {
		t.Fatal()
	}
	buf[0] = 0x00
	n.Read(bytes.NewReader(buf))
	if n.Value != false {
		t.Fatal()
	}
}

func TestAmfBoolean_Write(t *testing.T) {
	n := &AmfBoolean{true}
	buf := &bytes.Buffer{}
	n.Write(buf)
	if buf.Bytes()[0] != 0x01 {
		t.Fatal()
	}
	buf.Reset()
	n.Value = false
	n.Write(buf)
	if buf.Bytes()[0] != 0x00 {
		t.Fatal()
	}
}

func TestAmfBoolean_String(t *testing.T) {
	n := &AmfBoolean{true}
	if n.String() != "true" {
		t.Fatal()
	}
	n.Value = false
	if n.String() != "false" {
		t.Fatal()
	}
}

func TestAmfString_Id(t *testing.T) {
	n := &AmfString{}
	if n.Id() != String {
		t.Fatal()
	}
}

func TestAmfString_Read(t *testing.T) {
	n := &AmfString{}
	buf := []byte{0x00, 0x03, 'a', 'b', 'c'}
	n.Read(bytes.NewReader(buf))
	if n.Value != "abc" {
		t.Fatal()
	}
}

func TestAmfString_Write(t *testing.T) {
	n := &AmfString{"abc"}
	buf := &bytes.Buffer{}
	n.Write(buf)
	if binary.BigEndian.Uint16(buf.Bytes()[:2]) != 3 {
		t.Fatal()
	}
	if string(buf.Bytes()[2:]) != "abc" {
		t.Fatal()
	}
}

func TestAmfString_String(t *testing.T) {
	n := &AmfString{"abc"}
	if n.String() != "\"abc\"" {
		t.Fatal()
	}
}

func TestAmfObject_Id(t *testing.T) {
	n := &AmfObject{}
	if n.Id() != Object {
		t.Fatal()
	}
}

func TestAmfObject_Read(t *testing.T) {
	n := &AmfObject{}
	buf := []byte{
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, 0x09,
	}
	n.Read(bytes.NewReader(buf))
	if len(n.Value) != 1 {
		t.Fatal()
	}
	if n.Value["abc"].(*AmfString).Value != "efg" {
		t.Fatal()
	}
}

func TestAmfObject_Write(t *testing.T) {
	n := &AmfObject{map[string]Amf{"abc": &AmfString{"efg"}}}
	buf := &bytes.Buffer{}
	n.Write(buf)
	ref := []byte{
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	}
	if !bytes.Equal(buf.Bytes(), ref) {
		t.Fatal()
	}
}

func TestAmfObject_String(t *testing.T) {
	n := &AmfObject{map[string]Amf{"abc": &AmfString{"efg"}, "xxx" : &AmfString{"yyy"}}}
	str := n.String()
	if str != "{abc: \"efg\", xxx: \"yyy\"}" && str != "{xxx: \"yyy\", abc: \"efg\"}" {
		t.Fatal()
	}
}

func TestAmfNull_Id(t *testing.T) {
	n := &AmfNull{}
	if n.Id() != Null {
		t.Fatal()
	}
}

func TestAmfNull_Read(t *testing.T) {
	n := &AmfNull{}
	n.Read(nil)
}

func TestAmfNull_Write(t *testing.T) {
	n := &AmfNull{}
	n.Write(nil)
}

func TestAmfNull_String(t *testing.T) {
	n := &AmfNull{}
	if n.String() != "null" {
		t.Fatal()
	}
}

func TestAmfUndefined_Id(t *testing.T) {
	n := &AmfUndefined{}
	if n.Id() != Undefined {
		t.Fatal()
	}
}

func TestAmfUndefined_Read(t *testing.T) {
	n := &AmfUndefined{}
	n.Read(nil)
}

func TestAmfUndefined_Write(t *testing.T) {
	n := &AmfUndefined{}
	n.Write(nil)
}

func TestAmfUndefined_String(t *testing.T) {
	n := &AmfUndefined{}
	if n.String() != "undefined" {
		t.Fatal()
	}
}

func TestAmfArray_Id(t *testing.T) {
	n := &AmfArray{}
	if n.Id() != Array {
		t.Fatal()
	}
}

func TestAmfArray_Read(t *testing.T) {
	n := &AmfArray{}
	buf := []byte{
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	}
	n.Read(bytes.NewReader(buf))
	if len(n.Value) != 1 {
		t.Fatal()
	}
	if n.Value["abc"].(*AmfString).Value != "efg" {
		t.Fatal()
	}
}

func TestAmfArray_Write(t *testing.T) {
	n := &AmfArray{map[string]Amf{"abc": &AmfString{"efg"}}}
	buf := &bytes.Buffer{}
	n.Write(buf)
	ref := []byte{
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	}
	if !bytes.Equal(buf.Bytes(), ref) {
		t.Fatal()
	}
}

func TestAmfArray_String(t *testing.T) {
	n := &AmfArray{map[string]Amf{"aaa": &AmfString{"abc"}, "bbb": &AmfString{"xxx"}}}
	str := n.String()
	if str != "[aaa: \"abc\", bbb: \"xxx\"]" && str != "[bbb: \"xxx\", aaa: \"abc\"]" {
		t.Fatal()
	}
}

func TestAmfObjectEnd_Id(t *testing.T) {
	n := &AmfObjectEnd{}
	if n.Id() != ObjectEnd {
		t.Fatal()
	}
}

func TestAmfObjectEnd_Read(t *testing.T) {
	n := &AmfObjectEnd{}
	n.Read(nil)
}

func TestAmfObjectEnd_Write(t *testing.T) {
	n := &AmfObjectEnd{}
	n.Write(nil)
}

func TestAmfObjectEnd_String(t *testing.T) {
	n := &AmfObjectEnd{}
	if n.String() != "end" {
		t.Fatal()
	}
}

func TestAmfRead(t *testing.T) {
	buf := []byte{0xFF}
	if AmfRead(bytes.NewReader(buf)) != nil {
		t.Fatal()
	}
}

func TestAmfReadAll(t *testing.T) {
	num := make([]byte, 8)
	binary.BigEndian.PutUint64(num, math.Float64bits(3.14))
	buf := &bytes.Buffer{}
	buf.Write([]byte{Number})
	buf.Write(num)
	buf.Write([]byte{Boolean})
	buf.Write([]byte{0x01})
	buf.Write([]byte{String})
	buf.Write([]byte{0x00, 0x03, 'a', 'b', 'c'})
	buf.Write([]byte{Object})
	buf.Write([]byte{
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	})
	buf.Write([]byte{Null})
	buf.Write([]byte{Undefined})
	buf.Write([]byte{Array})
	buf.Write([]byte{
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	})
	amf := AmfReadAll(buf)
	if len(amf) != 7 {
		t.Fatal()
	}
	if amf[0].(*AmfNumber).Value != 3.14 {
		t.Fatal()
	}
	if amf[1].(*AmfBoolean).Value != true {
		t.Fatal()
	}
	if amf[2].(*AmfString).Value != "abc" {
		t.Fatal()
	}
	if amf[3].(*AmfObject).Value["abc"].(*AmfString).Value != "efg" {
		t.Fatal()
	}
	if amf[4].Id() != Null {
		t.Fatal()
	}
	if amf[5].Id() != Undefined {
		t.Fatal()
	}
	if amf[6].(*AmfArray).Value["abc"].(*AmfString).Value != "efg" {
		t.Fatal()
	}
}

func TestAmfWriteAll(t *testing.T) {
	buf := &bytes.Buffer{}
	AmfWriteAll(buf, []Amf{
		&AmfNumber{3.14},
		&AmfBoolean{true},
		&AmfString{"abc"},
		&AmfObject{map[string]Amf{"abc": &AmfString{"efg"}}},
		&AmfNull{},
		&AmfUndefined{},
		&AmfArray{map[string]Amf{"abc": &AmfString{"efg"}}},
	})

	num := make([]byte, 8)
	binary.BigEndian.PutUint64(num, math.Float64bits(3.14))
	ref := &bytes.Buffer{}
	ref.Write([]byte{Number})
	ref.Write(num)
	ref.Write([]byte{Boolean})
	ref.Write([]byte{0x01})
	ref.Write([]byte{String})
	ref.Write([]byte{0x00, 0x03, 'a', 'b', 'c'})
	ref.Write([]byte{Object})
	ref.Write([]byte{
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	})
	ref.Write([]byte{Null})
	ref.Write([]byte{Undefined})
	ref.Write([]byte{Array})
	ref.Write([]byte{
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		String, 0x00, 0x03, 'e', 'f', 'g',
		0x00, 0x00, ObjectEnd,
	})

	if !bytes.Equal(buf.Bytes(), ref.Bytes()) {
		t.Fatal()
	}
}