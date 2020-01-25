package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueTypedObject_Read(t *testing.T) {
	val := &ValueTypedObject{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x04, 'b', 'a', 'k', 'a',
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, map[string]Value{
		"abc": &ValueNull{},
	}, val.Data)
	assert.EqualValues(t, "baka", val.Name)
}

func TestValueTypedObject_Write(t *testing.T) {
	val := &ValueTypedObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
		},
		Name: "baka",
	}
	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerTypedObject),
		0x00, 0x04, 'b', 'a', 'k', 'a',
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}, buf.Bytes())
}

func TestValueTypedObject_String(t *testing.T) {
	val := &ValueTypedObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
		},
		Name: "baka",
	}
	assert.EqualValues(t, "baka{abc: null}", val.String())
}
