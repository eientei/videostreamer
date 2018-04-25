package amf

import (
	"io"
)

func (value *BooleanValue) Type() uint8 {
	return Boolean
}

func (value *BooleanValue) Read(reader io.Reader) error {
	buf := make([]byte, 1)
	if _, err := io.ReadFull(reader, buf); err != nil {
		return err
	} else {
		value.Data = buf[0] != 0
		return nil
	}
}

func (value *BooleanValue) Write(writer io.Writer) error {
	buf := make([]byte, 1)
	if value.Data {
		buf[0] = 1
	}
	_, err := writer.Write(buf)
	return err
}

func (value *BooleanValue) String() string {
	if value.Data {
		return "true"
	} else {
		return "false"
	}
}
