package amf0

import "io"

// ValueNull represents AMF0 null value
type ValueNull struct {
}

func (value *ValueNull) Read(reader io.Reader) error {
	return nil
}

func (value *ValueNull) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerNull)})
	return err
}

func (value *ValueNull) String() string {
	return "null"
}
