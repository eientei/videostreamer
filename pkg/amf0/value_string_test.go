package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueString_Read(t *testing.T) {
	val := &ValueString{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x03, 'a', 'b', 'c',
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, "abc", val.Data)
}

func TestValueString_Write(t *testing.T) {
	val := &ValueString{
		Data: "abc",
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerString),
		0x00, 0x03, 'a', 'b', 'c',
	}, buf.Bytes())
}

func TestValueString_String(t *testing.T) {
	assert.EqualValues(t, "\"abc\"", (&ValueString{
		Data: "abc",
	}).String())
}
