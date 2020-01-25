package amf0

import (
	"io"
)

// ValueObject represents AMF0 map object value
type ValueObject struct {
	Data map[string]Value
}

func (value *ValueObject) Read(reader io.Reader) error {
	data, err := readObject(reader)
	if err != nil {
		return err
	}

	value.Data = data

	return nil
}

func (value *ValueObject) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerObject)})
	if err != nil {
		return err
	}

	return writeObject(writer, value.Data)
}

func (value *ValueObject) String() string {
	return stringObject(value.Data)
}
