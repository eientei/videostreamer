package amf0

import (
	"io"
	"strings"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// ValueECMAArray represents AMF0 ordered associative array
type ValueECMAArray struct {
	Data []*Property
}

func (value *ValueECMAArray) Read(reader io.Reader) error {
	buf := make([]byte, 4)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	length := byteorder.BigEndian.Uint32(buf)
	value.Data = nil

	for i := 0; i < int(length); i++ {
		str, err := readUTF8(reader)
		if err != nil {
			return err
		}

		_, v, err := Unmarshal(reader)
		if err != nil {
			return err
		}

		value.Data = append(value.Data, &Property{
			Key:   str,
			Value: v,
		})
	}

	return nil
}

func (value *ValueECMAArray) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerECMAArray)})
	if err != nil {
		return err
	}

	buf := make([]byte, 4)

	byteorder.BigEndian.PutUint32(buf, uint32(len(value.Data)))

	_, err = writer.Write(buf)
	if err != nil {
		return err
	}

	for _, p := range value.Data {
		err = writeUTF8(writer, p.Key)
		if err != nil {
			return err
		}

		err = p.Value.Write(writer)
		if err != nil {
			return err
		}
	}

	return nil
}

func (value *ValueECMAArray) String() string {
	sb := &strings.Builder{}
	_, _ = sb.WriteString("[")

	for i, v := range value.Data {
		if i > 0 {
			_, _ = sb.WriteString(", ")
		}

		_, _ = sb.WriteString(v.Key)
		_, _ = sb.WriteString(": ")
		_, _ = sb.WriteString(v.Value.String())
	}

	_, _ = sb.WriteString("]")

	return sb.String()
}
