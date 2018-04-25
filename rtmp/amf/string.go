package amf

import (
	"io"
)

func (value *StringValue) Type() uint8 {
	return String
}

func (value *StringValue) Read(reader io.Reader) error {
	buf := make([]byte, 2)

	if _, err := io.ReadFull(reader, buf); err != nil {
		return err
	} else {
		data := make([]byte, ReadB16(buf))
		if _, err := io.ReadFull(reader, data); err != nil {
			return err
		} else {
			value.Data = string(data)
			return nil
		}
	}
}

func (value *StringValue) Write(writer io.Writer) error {
	buf := make([]byte, 2)
	WriteB16(buf, uint16(len(value.Data)))
	if _, err := writer.Write(buf); err != nil {
		return err
	} else {
		_, err := writer.Write([]byte(value.Data))
		return err
	}
}

func (value *StringValue) String() string {
	return "\"" + value.Data + "\""
}
