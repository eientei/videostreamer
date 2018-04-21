package amf

import "io"

func (value *EndValue) Type() uint8 {
	return End
}

func (value *EndValue) Read(reader io.Reader) error {
	return nil
}

func (value *EndValue) Write(writer io.Writer) error {
	return nil
}

func (value *EndValue) String() string {
	return "end"
}
