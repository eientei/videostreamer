package amf0

import (
	"io"
	"sort"
	"strings"

	"github.com/eientei/videostreamer/internal/byteorder"
)

func readUTF8(reader io.Reader) (string, error) {
	buf := make([]byte, 2)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return "", err
	}

	length := byteorder.BigEndian.Uint16(buf)
	raw := make([]byte, length)

	_, err = io.ReadFull(reader, raw)
	if err != nil {
		return "", err
	}

	return string(raw), nil
}
func writeUTF8(writer io.Writer, s string) error {
	raw := []byte(s)
	buf := make([]byte, 2)

	byteorder.BigEndian.PutUint16(buf, uint16(len(raw)))

	_, err := writer.Write(buf)
	if err != nil {
		return err
	}

	_, err = writer.Write(raw)

	return err
}

func readUTF8Long(reader io.Reader) (string, error) {
	buf := make([]byte, 4)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return "", err
	}

	length := byteorder.BigEndian.Uint32(buf)
	buf = make([]byte, length)

	_, err = io.ReadFull(reader, buf)
	if err != nil {
		return "", err
	}

	return string(buf), nil
}

func writeUTF8Long(writer io.Writer, s string) error {
	raw := []byte(s)
	buf := make([]byte, 4)

	byteorder.BigEndian.PutUint32(buf, uint32(len(raw)))

	_, err := writer.Write(buf)
	if err != nil {
		return err
	}

	_, err = writer.Write(raw)

	return err
}

func readObject(reader io.Reader) (data map[string]Value, err error) {
	data = make(map[string]Value)

	for {
		str, err := readUTF8(reader)
		if err != nil {
			return nil, err
		}

		marker, v, err := Unmarshal(reader)
		if err != nil {
			return nil, err
		}

		if marker == MarkerObjectEnd && len(str) == 0 {
			break
		}

		data[str] = v
	}

	return data, nil
}

func writeObject(writer io.Writer, data map[string]Value) (err error) {
	for k, v := range data {
		err = writeUTF8(writer, k)
		if err != nil {
			return err
		}

		err = v.Write(writer)
		if err != nil {
			return err
		}
	}

	err = writeUTF8(writer, "")
	if err != nil {
		return err
	}

	_, err = writer.Write([]byte{byte(MarkerObjectEnd)})

	return err
}

func stringObject(data map[string]Value) string {
	var keys []string

	for k := range data {
		keys = append(keys, k)
	}

	sort.Strings(keys)

	sb := &strings.Builder{}
	_, _ = sb.WriteString("{")

	for i, k := range keys {
		if i > 0 {
			_, _ = sb.WriteString(", ")
		}

		_, _ = sb.WriteString(k)
		_, _ = sb.WriteString(": ")
		_, _ = sb.WriteString(data[k].String())
	}

	_, _ = sb.WriteString("}")

	return sb.String()
}
