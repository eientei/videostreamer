package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueLongString_Read(t *testing.T) {
	val := &ValueLongString{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x00, 0x00, 0x03, 'a', 'b', 'c',
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, "abc", val.Data)
}

func TestValueLongString_Write(t *testing.T) {
	val := &ValueLongString{
		Data: "abc",
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerLongString),
		0x00, 0x00, 0x00, 0x03, 'a', 'b', 'c',
	}, buf.Bytes())
}

func TestValueLongString_String(t *testing.T) {
	assert.EqualValues(t, "\"abc\"", (&ValueLongString{
		Data: "abc",
	}).String())
}
