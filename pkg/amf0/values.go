package amf0

import (
	"fmt"
	"io"
)

// Value interface provides methods for reading, writing and printing AMF0 value
type Value interface {
	Read(reader io.Reader) error
	Write(writer io.Writer) error
	fmt.Stringer
}

// Property key-value pair used in objects and associative array
type Property struct {
	Key   string
	Value Value
}
