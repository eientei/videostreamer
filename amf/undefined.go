package amf

import "io"

func (value *UndefinedValue) Type() uint8 {
	return Undefined
}

func (value *UndefinedValue) Read(reader io.Reader) error {
	return nil
}

func (value *UndefinedValue) Write(writer io.Writer) error {
	return nil
}

func (value *UndefinedValue) String() string {
	return "undefined"
}
