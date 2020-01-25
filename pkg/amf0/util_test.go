package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestUtil_readUTF8(t *testing.T) {
	buf := []byte{
		0x00, 0x03, 'a', 'b', 'c',
	}
	str, err := readUTF8(bytes.NewReader(buf))
	assert.NoError(t, err)
	assert.EqualValues(t, "abc", str)
}

func TestUtil_writeUTF8(t *testing.T) {
	buf := &bytes.Buffer{}
	err := writeUTF8(buf, "abc")
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		0x00, 0x03, 'a', 'b', 'c',
	}, buf.Bytes())
}

func TestUtil_readUTF8Long(t *testing.T) {
	buf := []byte{
		0x00, 0x00, 0x00, 0x03, 'a', 'b', 'c',
	}
	str, err := readUTF8Long(bytes.NewReader(buf))
	assert.NoError(t, err)
	assert.EqualValues(t, "abc", str)
}

func TestUtil_writeUTF8Long(t *testing.T) {
	buf := &bytes.Buffer{}
	err := writeUTF8Long(buf, "abc")
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		0x00, 0x00, 0x00, 0x03, 'a', 'b', 'c',
	}, buf.Bytes())
}

func TestUtil_readObject(t *testing.T) {
	buf := []byte{
		0x00, 0x03, 'a', 'b', 'c', byte(MarkerNull),
		0x00, 0x00, byte(MarkerObjectEnd),
	}
	data, err := readObject(bytes.NewReader(buf))
	assert.NoError(t, err)
	assert.EqualValues(t, map[string]Value{"abc": &ValueNull{}}, data)
}

func TestUtil_writeObject(t *testing.T) {
	buf := &bytes.Buffer{}
	err := writeObject(buf, map[string]Value{"abc": &ValueNull{}})
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		0x00, 0x03, 'a', 'b', 'c', byte(MarkerNull),
		0x00, 0x00, byte(MarkerObjectEnd),
	}, buf.Bytes())
}

func TestUtil_stringObject(t *testing.T) {
	str := stringObject(map[string]Value{"abc": &ValueNull{}})
	assert.EqualValues(t, "{abc: null}", str)
}
