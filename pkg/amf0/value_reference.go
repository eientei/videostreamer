package amf0

import (
	"io"
	"strconv"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// ValueReference represents AMF0 ordinal reference to another [referencable] value
type ValueReference struct {
	Data uint16
}

func (value *ValueReference) Read(reader io.Reader) error {
	buf := make([]byte, 2)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	value.Data = byteorder.BigEndian.Uint16(buf)

	return nil
}

func (value *ValueReference) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerReference)})
	if err != nil {
		return err
	}

	buf := make([]byte, 2)

	byteorder.BigEndian.PutUint16(buf, value.Data)

	_, err = writer.Write(buf)

	return err
}

func (value *ValueReference) String() string {
	return "&" + strconv.FormatUint(uint64(value.Data), 10)
}
