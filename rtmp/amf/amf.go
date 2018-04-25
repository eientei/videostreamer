package amf

import (
	"errors"
	"fmt"
	"io"
)

var UnknownTag = errors.New("unknown tag")

const (
	Number    = 0x00
	Boolean   = 0x01
	String    = 0x02
	Object    = 0x03
	Null      = 0x05
	Undefined = 0x06
	Array     = 0x08
	End       = 0x09
)

type Value interface {
	fmt.Stringer
	Type() uint8
	Read(reader io.Reader) error
	Write(writer io.Writer) error
}

type NumberValue struct {
	Data float64
}

type BooleanValue struct {
	Data bool
}

type StringValue struct {
	Data string
}

type ObjectValue struct {
	Data map[string]Value
}

type NullValue struct {
}

type UndefinedValue struct {
}

type ArrayValue struct {
	Data map[string]Value
}

type EndValue struct {
}

func Read(reader io.Reader) (Value, error) {
	idbuf := make([]byte, 1)
	if _, err := reader.Read(idbuf); err != nil {
		return nil, err
	}
	var val Value
	switch idbuf[0] {
	case Number:
		val = &NumberValue{}
	case Boolean:
		val = &BooleanValue{}
	case String:
		val = &StringValue{}
	case Object:
		val = &ObjectValue{}
	case Null:
		val = &NullValue{}
	case Undefined:
		val = &UndefinedValue{}
	case Array:
		val = &ArrayValue{}
	case End:
		val = &EndValue{}
	default:
		return nil, UnknownTag
	}

	if err := val.Read(reader); err != nil {
		return nil, err
	}

	return val, nil
}

func Write(writer io.Writer, value Value) error {
	if _, err := writer.Write([]byte{value.Type()}); err != nil {
		return err
	}
	return value.Write(writer)
}

func ReadAll(reader io.Reader) []Value {
	arr := make([]Value, 0)
	for {
		if v, err := Read(reader); err != nil {
			break
		} else {
			arr = append(arr, v)
		}
	}
	return arr
}

func WriteAll(writer io.Writer, arr []Value) error {
	for _, v := range arr {
		if err := Write(writer, v); err != nil {
			return err
		}
	}
	return nil
}
