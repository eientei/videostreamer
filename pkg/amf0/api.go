// Package amf0 provides AMF0 serializing and deserializing utilities
package amf0

import (
	"bytes"
	"errors"
	"fmt"
	"io"
)

var (
	// ErrUnsupportedMarker is returned when unrecognized marker is met in AMF0 stream
	ErrUnsupportedMarker = errors.New("unsupported marker")
)

// ErrorReadingMarker is returned when EOF occurred during reading marker byte
type ErrorReadingMarker struct {
	msg string
	err error
}

// Error interface
func (e *ErrorReadingMarker) Error() string {
	return e.msg
}

// Unwrap underlying error
func (e *ErrorReadingMarker) Unwrap() error {
	return e.err
}

// Unmarshal reads single complete AMF0 entity from given reader
func Unmarshal(reader io.Reader) (marker Marker, val Value, err error) {
	buf := make([]byte, 1)

	_, err = reader.Read(buf)
	if err != nil {
		if errors.Is(err, io.EOF) || errors.Is(err, io.ErrUnexpectedEOF) {
			return MarkerInvalid, nil, &ErrorReadingMarker{
				msg: fmt.Sprintf("error reading marker: %v", err.Error()),
				err: err,
			}
		}

		return MarkerInvalid, nil, err
	}

	marker = Marker(buf[0])

	switch marker {
	case MarkerNumber:
		val = &ValueNumber{}
	case MarkerBoolean:
		val = &ValueBoolean{}
	case MarkerString:
		val = &ValueString{}
	case MarkerObject:
		val = &ValueObject{}
	case MarkerNull:
		val = &ValueNull{}
	case MarkerUndefined:
		val = &ValueUndefined{}
	case MarkerReference:
		val = &ValueReference{}
	case MarkerECMAArray:
		val = &ValueECMAArray{}
	case MarkerStrictArray:
		val = &ValueStrictArray{}
	case MarkerDate:
		val = &ValueDate{}
	case MarkerLongString:
		val = &ValueLongString{}
	case MarkerXMLDocument:
		val = &ValueXMLDocument{}
	case MarkerTypedObject:
		val = &ValueTypedObject{}
	case MarkerObjectEnd:
		return marker, nil, nil
	default:
		return marker, nil, ErrUnsupportedMarker
	}

	err = val.Read(reader)
	if err != nil {
		return marker, nil, err
	}

	return
}

// Marshal writes given value to given writer
func Marshal(writer io.Writer, val Value) (err error) {
	err = val.Write(writer)
	return
}

// UnmarshalAll reads all AMF0 entities from given reader until EOF or other error
func UnmarshalAll(reader io.Reader) (values []Value, err error) {
	for {
		_, val, err := Unmarshal(reader)
		if err != nil {
			if _, r := err.(*ErrorReadingMarker); r {
				return values, nil
			}

			return nil, err
		}

		values = append(values, val)
	}
}

// MarshalAll writes given array of values to given writer
func MarshalAll(writer io.Writer, values ...Value) (err error) {
	for _, v := range values {
		err = v.Write(writer)
		if err != nil {
			return err
		}
	}

	return nil
}

// UnmarshalAllBytes reads all AMF0 entities from given byte array
func UnmarshalAllBytes(b []byte) (values []Value, err error) {
	return UnmarshalAll(bytes.NewReader(b))
}

// MarshalAllBytes writes all given values to a byte array
func MarshalAllBytes(values ...Value) (b []byte, err error) {
	buf := &bytes.Buffer{}

	err = MarshalAll(buf, values...)
	if err != nil {
		return nil, err
	}

	return buf.Bytes(), nil
}
