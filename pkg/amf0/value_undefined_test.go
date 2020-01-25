package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueUndefined_Read(t *testing.T) {
	val := &ValueUndefined{}
	err := val.Read(bytes.NewReader([]byte{}))
	assert.NoError(t, err)
}

func TestValueUndefined_Write(t *testing.T) {
	val := &ValueUndefined{}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerUndefined),
	}, buf.Bytes())
}

func TestValueUndefined_String(t *testing.T) {
	assert.EqualValues(t, "undefined", (&ValueUndefined{}).String())
}
