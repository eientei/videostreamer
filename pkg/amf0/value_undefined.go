package amf0

import "io"

// ValueUndefined represents AMF0 undefined value
type ValueUndefined struct {
}

func (value *ValueUndefined) Read(reader io.Reader) error {
	return nil
}

func (value *ValueUndefined) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerUndefined)})
	return err
}

func (value *ValueUndefined) String() string {
	return "undefined"
}
