package amf0

import (
	"io"
)

// ValueBoolean represents AMF0 boolean value
type ValueBoolean struct {
	Data bool
}

func (value *ValueBoolean) Read(reader io.Reader) error {
	buf := make([]byte, 1)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	value.Data = buf[0] != 0

	return nil
}

func (value *ValueBoolean) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerBoolean)})
	if err != nil {
		return err
	}

	buf := make([]byte, 1)

	if value.Data {
		buf[0] = 1
	}

	_, err = writer.Write(buf)

	return err
}

func (value *ValueBoolean) String() string {
	if value.Data {
		return "true"
	}

	return "false"
}
