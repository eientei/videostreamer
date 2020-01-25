package amf0

import (
	"bytes"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestValueDate_Read(t *testing.T) {
	val := &ValueDate{}
	err := val.Read(bytes.NewReader([]byte{
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00,
	}))
	assert.NoError(t, err)
	assert.EqualValues(t, time.Unix(0, 0), val.Data)
}

func TestValueDate_Write(t *testing.T) {
	val := &ValueDate{
		Data: time.Unix(0, 0),
	}

	buf := &bytes.Buffer{}
	err := val.Write(buf)
	assert.NoError(t, err)
	assert.EqualValues(t, []byte{
		byte(MarkerDate),
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00,
	}, buf.Bytes())
}

func TestValueDate_String(t *testing.T) {
	assert.EqualValues(t, time.Unix(0, 0).Format(time.RFC3339), (&ValueDate{
		Data: time.Unix(0, 0),
	}).String())
}
