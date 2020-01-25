package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueBoolean_Read(t *testing.T) {
	val := &ValueBoolean{}
	err := val.Read(bytes.NewReader([]byte{
		0x01,
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, true, val.Data)
	err = val.Read(bytes.NewReader([]byte{
		0x00,
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, false, val.Data)
}

func TestValueBoolean_Write(t *testing.T) {
	val := &ValueBoolean{
		Data: true,
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerBoolean),
		0x01,
	}, buf.Bytes())

	val.Data = false
	buf.Reset()
	err = val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerBoolean),
		0x00,
	}, buf.Bytes())
}

func TestValueBoolean_String(t *testing.T) {
	assert.EqualValues(t, "true", (&ValueBoolean{
		Data: true,
	}).String())
	assert.EqualValues(t, "false", (&ValueBoolean{
		Data: false,
	}).String())
}
