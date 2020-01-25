package amf0

import (
	"io"
	"strings"
)

var stringReplacer = strings.NewReplacer(
	"\\", "\\\\",
	"\"", "\\\"",
)

// ValueString represents AMF0 string with 16-bit length
type ValueString struct {
	Data string
}

func (value *ValueString) Read(reader io.Reader) error {
	str, err := readUTF8(reader)
	if err != nil {
		return err
	}

	value.Data = str

	return nil
}

func (value *ValueString) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerString)})
	if err != nil {
		return err
	}

	return writeUTF8(writer, value.Data)
}

func (value *ValueString) String() string {
	return "\"" + stringReplacer.Replace(value.Data) + "\""
}
