package server

import (
	"io"
	"encoding/binary"
	"math"
	"fmt"
	"strconv"
	"bytes"
)

const (
	Number = 0x00
	Boolean = 0x01
	String = 0x02
	Object = 0x03
	Null = 0x05
	Undefined = 0x06
	Array = 0x08
	ObjectEnd = 0x09
)

type Amf interface {
	Id() uint8
	Read(reader io.Reader)
	Write(writer io.Writer)
	fmt.Stringer
}

type AmfNumber struct {
	Value float64
}

func (value *AmfNumber) Id() uint8 {
	return Number
}

func (value *AmfNumber) Read(reader io.Reader) {
	buf := make([]byte, 8)
	reader.Read(buf)
	value.Value = math.Float64frombits(binary.BigEndian.Uint64(buf))
}

func (value *AmfNumber) Write(writer io.Writer) {
	buf := make([]byte, 8)
	binary.BigEndian.PutUint64(buf, math.Float64bits(value.Value))
	writer.Write(buf)
}

func (value *AmfNumber) String() string {
	return strconv.FormatFloat(value.Value, 'f', -1, 64)
}

type AmfBoolean struct {
	Value bool
}

func (value *AmfBoolean) Id() uint8 {
	return Boolean
}

func (value *AmfBoolean) Read(reader io.Reader) {
	buf := make([]byte, 1)
	reader.Read(buf)
	value.Value = buf[0] != 0x00
}

func (value *AmfBoolean) Write(writer io.Writer) {
	v := byte(0x00)
	if value.Value {
		v = byte(0x01)
	}
	writer.Write([]byte{v})
}

func (value *AmfBoolean) String() string {
	if value.Value {
		return "true"
	} else {
		return "false"
	}
}

type AmfString struct {
	Value string
}

func (value *AmfString) Id() uint8 {
	return String
}

func (value *AmfString) Read(reader io.Reader) {
	lenbuf := make([]byte, 2)
	reader.Read(lenbuf)
	l := binary.BigEndian.Uint16(lenbuf)
	strbuf := make([]byte, l)
	reader.Read(strbuf)
	value.Value = string(strbuf)
}

func (value *AmfString) Write(writer io.Writer) {
	lenbuf := make([]byte, 2)
	binary.BigEndian.PutUint16(lenbuf, uint16(len(value.Value)))
	writer.Write(lenbuf)
	writer.Write([]byte(value.Value))
}

func (value *AmfString) String() string {
	return "\"" + value.Value + "\""
}

type AmfObject struct {
	Value map[string]Amf
}

func (value *AmfObject) Id() uint8 {
	return Object
}

func (value *AmfObject) Read(reader io.Reader) {
	value.Value = make(map[string]Amf)
	for {
		key := &AmfString{}
		key.Read(reader)
		val := AmfRead(reader)
		str := key.Value
		if len(str) == 0 && val.Id() == ObjectEnd {
			break
		}
		value.Value[str] = val
	}
}

func (value *AmfObject) Write(writer io.Writer) {
	name := &AmfString{}
	for key, value := range value.Value {
		name.Value = key
		name.Write(writer)
		AmfWrite(writer, value)
	}
	writer.Write([]byte{0x00, 0x00, ObjectEnd})
}

func (value *AmfObject) String() string {
	buf := &bytes.Buffer{}
	buf.WriteString("{")
	fst := true
	for k, v := range value.Value {
		if fst {
			fst = false
		} else {
			buf.WriteString(", ")
		}
		buf.WriteString(k)
		buf.WriteString(": ")
		buf.WriteString(v.String())
	}
	buf.WriteString("}")
	return buf.String()
}

type AmfNull struct {
}

func (value *AmfNull) Id() uint8 {
	return Null
}

func (value *AmfNull) Read(reader io.Reader) {

}

func (value *AmfNull) Write(writer io.Writer) {

}

func (value *AmfNull) String() string {
	return "null"
}

type AmfUndefined struct {
}

func (value *AmfUndefined) Id() uint8 {
	return Undefined
}

func (value *AmfUndefined) Read(reader io.Reader) {

}

func (value *AmfUndefined) Write(writer io.Writer) {

}

func (value *AmfUndefined) String() string {
	return "undefined"
}

type AmfArray struct {
	Value map[string]Amf
}

func (value *AmfArray) Id() uint8 {
	return Array
}

func (value *AmfArray) Read(reader io.Reader) {
	lenbuf := make([]byte, 4)
	reader.Read(lenbuf)
	l := binary.BigEndian.Uint32(lenbuf)
	value.Value = make(map[string]Amf)
	for i := uint32(0); i < l; i++ {
		key := &AmfString{}
		key.Read(reader)
		value.Value[key.Value] = AmfRead(reader)
	}
	termbuf := make([]byte, 3)
	reader.Read(termbuf)
}

func (value *AmfArray) Write(writer io.Writer) {
	lenbuf := make([]byte, 4)
	binary.BigEndian.PutUint32(lenbuf, uint32(len(value.Value)))
	writer.Write(lenbuf)
	name := &AmfString{}
	for k, v := range value.Value {
		name.Value = k
		name.Write(writer)
		AmfWrite(writer, v)
	}
	writer.Write([]byte{0x00, 0x00, ObjectEnd})
}

func (value *AmfArray) String() string {
	buf := &bytes.Buffer{}
	buf.WriteString("[")
	fst := true
	for k, v := range value.Value {
		if fst {
			fst = false
		} else {
			buf.WriteString(", ")
		}
		buf.WriteString(k)
		buf.WriteString(": ")
		buf.WriteString(v.String())
	}
	buf.WriteString("]")
	return buf.String()
}

type AmfObjectEnd struct {
}

func (value *AmfObjectEnd) Id() uint8 {
	return ObjectEnd
}

func (value *AmfObjectEnd) Read(reader io.Reader) {

}

func (value *AmfObjectEnd) Write(writer io.Writer) {

}

func (value *AmfObjectEnd) String() string {
	return "end"
}

func AmfRead(reader io.Reader) Amf {
	idbuf := make([]byte, 1)
	if _, err := reader.Read(idbuf); err != nil {
		return nil
	}
	switch idbuf[0] {
	case Number:
		val := &AmfNumber{}
		val.Read(reader)
		return val
	case Boolean:
		val := &AmfBoolean{}
		val.Read(reader)
		return val
	case String:
		val := &AmfString{}
		val.Read(reader)
		return val
	case Object:
		val := &AmfObject{}
		val.Read(reader)
		return val
	case Null:
		val := &AmfNull{}
		val.Read(reader)
		return val
	case Undefined:
		val := &AmfUndefined{}
		val.Read(reader)
		return val
	case Array:
		val := &AmfArray{}
		val.Read(reader)
		return val
	case ObjectEnd:
		val := &AmfObjectEnd{}
		val.Read(reader)
		return val
	}
	return nil
}

func AmfWrite(writer io.Writer, value Amf) {
	writer.Write([]byte{value.Id()})
	value.Write(writer)
}

func AmfReadAll(reader io.Reader) []Amf {
	arr := make([]Amf, 0)
	for {
		v := AmfRead(reader)
		if v == nil {
			break
		}
		arr = append(arr, v)
	}
	return arr
}

func AmfWriteAll(writer io.Writer, arr []Amf) {
	for _, v := range arr {
		AmfWrite(writer, v)
	}
}