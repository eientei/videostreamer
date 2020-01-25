package amf0

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValueXMLDocument_Read(t *testing.T) {
	val := &ValueXMLDocument{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x00, 0x00, 0x06, '<', 'x', 'm', 'l', '/', '>',
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, "<xml/>", val.Data)
}

func TestValueXMLDocument_Write(t *testing.T) {
	val := &ValueXMLDocument{
		Data: "<xml/>",
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerXMLDocument),
		0x00, 0x00, 0x00, 0x06, '<', 'x', 'm', 'l', '/', '>',
	}, buf.Bytes())
}

func TestValueXMLDocument_String(t *testing.T) {
	assert.EqualValues(t, "@@@XML@@@<xml/>@@@/XML@@@", (&ValueXMLDocument{
		Data: "<xml/>",
	}).String())
}
