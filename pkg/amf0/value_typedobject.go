package amf0

import (
	"io"
)

// ValueTypedObject represents AMF0 named object type
type ValueTypedObject struct {
	Data map[string]Value
	Name string
}

func (value *ValueTypedObject) Read(reader io.Reader) error {
	str, err := readUTF8(reader)
	if err != nil {
		return err
	}

	value.Name = str

	data, err := readObject(reader)
	if err != nil {
		return err
	}

	value.Data = data

	return nil
}

func (value *ValueTypedObject) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerTypedObject)})
	if err != nil {
		return err
	}

	err = writeUTF8(writer, value.Name)
	if err != nil {
		return err
	}

	return writeObject(writer, value.Data)
}

func (value *ValueTypedObject) String() string {
	return value.Name + stringObject(value.Data)
}
