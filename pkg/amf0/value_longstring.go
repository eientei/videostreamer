package amf0

import (
	"io"
)

// ValueLongString represents AMF0 string with 32-bit length
type ValueLongString struct {
	Data string
}

func (value *ValueLongString) Read(reader io.Reader) error {
	str, err := readUTF8Long(reader)
	if err != nil {
		return err
	}

	value.Data = str

	return nil
}

func (value *ValueLongString) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerLongString)})
	if err != nil {
		return err
	}

	return writeUTF8Long(writer, value.Data)
}

func (value *ValueLongString) String() string {
	return "\"" + stringReplacer.Replace(value.Data) + "\""
}
