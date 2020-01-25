package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueStrictArray_Read(t *testing.T) {
	val := &ValueStrictArray{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x00, 0x00, 0x01,
		byte(MarkerNull),
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, []Value{
		&ValueNull{},
	}, val.Data)
}

func TestValueStrictArray_Write(t *testing.T) {
	val := &ValueStrictArray{
		Data: []Value{
			&ValueNull{},
		},
	}
	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerStrictArray),
		0x00, 0x00, 0x00, 0x01,
		byte(MarkerNull),
	}, buf.Bytes())
}

func TestValueStrictArray_String(t *testing.T) {
	val := &ValueStrictArray{
		Data: []Value{
			&ValueNull{},
		},
	}
	assert.EqualValues(t, "[null]", val.String())
}
