package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueECMAArray_Read(t *testing.T) {
	val := &ValueECMAArray{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, []*Property{
		{
			Key:   "abc",
			Value: &ValueNull{},
		},
	}, val.Data)
}

func TestValueECMAArray_Write(t *testing.T) {
	val := &ValueECMAArray{
		Data: []*Property{
			{
				Key:   "abc",
				Value: &ValueNull{},
			},
		},
	}
	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerECMAArray),
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
	}, buf.Bytes())
}

func TestValueECMAArray_String(t *testing.T) {
	val := &ValueECMAArray{
		Data: []*Property{
			{
				Key:   "abc",
				Value: &ValueNull{},
			},
		},
	}
	assert.EqualValues(t, "[abc: null]", val.String())
}
