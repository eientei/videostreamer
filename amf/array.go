package amf

import (
	"bytes"
	"errors"
	"io"
	"sort"

	"github.com/eientei/videostreamer/util"
)

var InvalidArray = errors.New("invalid array")

func (value *ArrayValue) Type() uint8 {
	return Array
}

func (value *ArrayValue) Read(reader io.Reader) error {
	buf := make([]byte, 4)
	if _, err := io.ReadFull(reader, buf); err != nil {
		return err
	}
	value.Data = make(map[string]Value)
	name := &StringValue{}
	for {
		if err := name.Read(reader); err != nil {
			return err
		}
		key := name.Data
		if val, err := Read(reader); err != nil {
			return err
		} else {
			if len(key) == 0 && val.Type() == End {
				break
			}
			value.Data[key] = val
		}
	}
	return nil
}

func (value *ArrayValue) Write(writer io.Writer) error {
	buf := make([]byte, 4)
	util.WriteB32(buf, uint32(len(value.Data)))
	if _, err := writer.Write(buf); err != nil {
		return err
	}
	name := &StringValue{}
	for key, val := range value.Data {
		name.Data = key
		if err := name.Write(writer); err != nil {
			return err
		}
		if err := Write(writer, val); err != nil {
			return err
		}
	}
	name.Data = ""
	if err := name.Write(writer); err != nil {
		return err
	}
	if err := Write(writer, &EndValue{}); err != nil {
		return err
	}
	return nil
}

func (value *ArrayValue) String() string {
	keys := make([]string, 0)
	for key := range value.Data {
		keys = append(keys, key)
	}
	sort.Strings(keys)

	buf := &bytes.Buffer{}
	buf.WriteString("[")
	for i, key := range keys {
		if i > 0 {
			buf.WriteString(", ")
		}
		buf.WriteString(key)
		buf.WriteString(": ")
		buf.WriteString(value.Data[key].String())
	}
	buf.WriteString("]")
	return buf.String()
}
