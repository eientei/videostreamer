package amf0

import (
	"io"
	"strings"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// ValueStrictArray represents AMF0 sequential array
type ValueStrictArray struct {
	Data []Value
}

func (value *ValueStrictArray) Read(reader io.Reader) error {
	buf := make([]byte, 4)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	length := byteorder.BigEndian.Uint32(buf)

	value.Data = nil

	for i := 0; i < int(length); i++ {
		_, v, err := Unmarshal(reader)
		if err != nil {
			return err
		}

		value.Data = append(value.Data, v)
	}

	return nil
}

func (value *ValueStrictArray) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerStrictArray)})
	if err != nil {
		return err
	}

	buf := make([]byte, 4)

	byteorder.BigEndian.PutUint32(buf, uint32(len(value.Data)))

	_, err = writer.Write(buf)
	if err != nil {
		return err
	}

	for _, v := range value.Data {
		err = v.Write(writer)
		if err != nil {
			return err
		}
	}

	return nil
}

func (value *ValueStrictArray) String() string {
	sb := &strings.Builder{}
	_, _ = sb.WriteString("[")

	for i, v := range value.Data {
		if i > 0 {
			_, _ = sb.WriteString(", ")
		}

		_, _ = sb.WriteString(v.String())
	}

	_, _ = sb.WriteString("]")

	return sb.String()
}
