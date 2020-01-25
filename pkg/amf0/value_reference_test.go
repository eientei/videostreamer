package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueReference_Read(t *testing.T) {
	val := &ValueReference{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x01,
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, 1, val.Data)
}

func TestValueReference_Write(t *testing.T) {
	val := &ValueReference{
		Data: 1,
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerReference),
		0x00, 0x01,
	}, buf.Bytes())
}

func TestValueReference_String(t *testing.T) {
	assert.EqualValues(t, "&1", (&ValueReference{
		Data: 1,
	}).String())
}
