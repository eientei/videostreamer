package amf0

import (
	"io"
)

// ValueXMLDocument represents AMF0 xml document string value with 32-bit length
type ValueXMLDocument struct {
	Data string
}

func (value *ValueXMLDocument) Read(reader io.Reader) error {
	str, err := readUTF8Long(reader)
	if err != nil {
		return err
	}

	value.Data = str

	return nil
}

func (value *ValueXMLDocument) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerXMLDocument)})
	if err != nil {
		return err
	}

	return writeUTF8Long(writer, value.Data)
}

func (value *ValueXMLDocument) String() string {
	return "@@@XML@@@" + value.Data + "@@@/XML@@@"
}
