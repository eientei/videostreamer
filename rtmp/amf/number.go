package amf

import (
	"io"
	"math"
	"strconv"
)

func (value *NumberValue) Type() uint8 {
	return Number
}

func (value *NumberValue) Read(reader io.Reader) error {
	buf := make([]byte, 8)

	if _, err := io.ReadFull(reader, buf); err != nil {
		return err
	} else {
		value.Data = math.Float64frombits(ReadB64(buf))
		return nil
	}
}

func (value *NumberValue) Write(writer io.Writer) error {
	buf := make([]byte, 8)
	WriteB64(buf, math.Float64bits(value.Data))
	_, err := writer.Write(buf)
	return err
}

func (value *NumberValue) String() string {
	return strconv.FormatFloat(value.Data, 'f', -1, 64)
}
