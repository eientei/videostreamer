package amf

import (
	"bytes"
	"io"
	"sort"
)

func (value *ObjectValue) Type() uint8 {
	return Object
}

func (value *ObjectValue) Read(reader io.Reader) error {
	value.Data = make(map[string]Value)
	name := &StringValue{}
	for {
		if err := name.Read(reader); err != nil {
			return err
		}
		if val, err := Read(reader); err != nil {
			return err
		} else {
			key := name.Data
			if len(key) == 0 && val.Type() == End {
				break
			}
			value.Data[key] = val
		}
	}
	return nil
}

func (value *ObjectValue) Write(writer io.Writer) error {
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

func (value *ObjectValue) String() string {
	keys := make([]string, 0)
	for key := range value.Data {
		keys = append(keys, key)
	}
	sort.Strings(keys)

	buf := &bytes.Buffer{}
	buf.WriteString("{")
	for i, key := range keys {
		if i > 0 {
			buf.WriteString(", ")
		}
		buf.WriteString(key)
		buf.WriteString(": ")
		buf.WriteString(value.Data[key].String())
	}
	buf.WriteString("}")
	return buf.String()
}
