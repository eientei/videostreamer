package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueNull_Read(t *testing.T) {
	val := &ValueNull{}
	err := val.Read(bytes.NewReader([]byte{}))
	assert.NoError(t, err)
}

func TestValueNull_Write(t *testing.T) {
	val := &ValueNull{}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerNull),
	}, buf.Bytes())
}

func TestValueNull_String(t *testing.T) {
	assert.EqualValues(t, "null", (&ValueNull{}).String())
}
