package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueNumber_Read(t *testing.T) {
	val := &ValueNumber{}
	err := val.Read(bytes.NewReader([]byte{
		0x40, 0x28, 0xAE, 0x14, 0x7A, 0xE1, 0x47, 0xAE,
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, 12.34, val.Data)
}

func TestValueNumber_Write(t *testing.T) {
	val := &ValueNumber{
		Data: 12.34,
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerNumber),
		0x40, 0x28, 0xAE, 0x14, 0x7A, 0xE1, 0x47, 0xAE,
	}, buf.Bytes())
}

func TestValueNumber_String(t *testing.T) {
	assert.EqualValues(t, "12.34", (&ValueNumber{
		Data: 12.34,
	}).String())
}
