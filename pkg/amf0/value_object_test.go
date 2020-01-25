package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueObject_Read(t *testing.T) {
	val := &ValueObject{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, map[string]Value{
		"abc": &ValueNull{},
	}, val.Data)
}

func TestValueObject_Write(t *testing.T) {
	val := &ValueObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
		},
	}
	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerObject),
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}, buf.Bytes())
}

func TestValueObject_String(t *testing.T) {
	val := &ValueObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
		},
	}
	assert.EqualValues(t, "{abc: null}", val.String())
}
