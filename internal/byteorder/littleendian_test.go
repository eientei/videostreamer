package byteorder

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestLittleEndianImpl_Uint16(t *testing.T) {
	assert.EqualValues(t, 3106, LittleEndian.Uint16([]byte{34, 12}))
}

func TestLittleEndianImpl_Uint24(t *testing.T) {
	assert.EqualValues(t, 795192, LittleEndian.Uint24([]byte{56, 34, 12}))
}

func TestLittleEndianImpl_Uint32(t *testing.T) {
	assert.EqualValues(t, 203569230, LittleEndian.Uint32([]byte{78, 56, 34, 12}))
}

func TestLittleEndianImpl_Uint64(t *testing.T) {
	assert.EqualValues(t, 874323186832646712, LittleEndian.Uint64([]byte{56, 34, 12, 90, 78, 56, 34, 12}))
}

func TestLittleEndianImpl_PutUint16(t *testing.T) {
	buf := make([]byte, 2)
	LittleEndian.PutUint16(buf, 3106)
	assert.EqualValues(t, []byte{34, 12}, buf)
}

func TestLittleEndianImpl_PutUint24(t *testing.T) {
	buf := make([]byte, 3)
	LittleEndian.PutUint24(buf, 795192)
	assert.EqualValues(t, []byte{56, 34, 12}, buf)
}

func TestLittleEndianImpl_PutUint32(t *testing.T) {
	buf := make([]byte, 4)
	LittleEndian.PutUint32(buf, 203569230)
	assert.EqualValues(t, []byte{78, 56, 34, 12}, buf)
}

func TestLittleEndianImpl_PutUint64(t *testing.T) {
	buf := make([]byte, 8)
	LittleEndian.PutUint64(buf, 874323186832646712)
	assert.EqualValues(t, []byte{56, 34, 12, 90, 78, 56, 34, 12}, buf)
}

func TestLittleEndianImpl_String(t *testing.T) {
	assert.NotEmpty(t, LittleEndian.String())
}
