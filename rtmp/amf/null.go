package amf

import "io"

func (value *NullValue) Type() uint8 {
	return Null
}

func (value *NullValue) Read(reader io.Reader) error {
	return nil
}

func (value *NullValue) Write(writer io.Writer) error {
	return nil
}

func (value *NullValue) String() string {
	return "null"
}
