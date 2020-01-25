package byteorder

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBigEndianImpl_Uint16(t *testing.T) {
	assert.EqualValues(t, 3106, BigEndian.Uint16([]byte{12, 34}))
}

func TestBigEndianImpl_Uint24(t *testing.T) {
	assert.EqualValues(t, 795192, BigEndian.Uint24([]byte{12, 34, 56}))
}

func TestBigEndianImpl_Uint32(t *testing.T) {
	assert.EqualValues(t, 203569230, BigEndian.Uint32([]byte{12, 34, 56, 78}))
}

func TestBigEndianImpl_Uint64(t *testing.T) {
	assert.EqualValues(t, 874323186832646712, BigEndian.Uint64([]byte{12, 34, 56, 78, 90, 12, 34, 56}))
}

func TestBigEndianImpl_PutUint16(t *testing.T) {
	buf := make([]byte, 2)
	BigEndian.PutUint16(buf, 3106)
	assert.EqualValues(t, []byte{12, 34}, buf)
}

func TestBigEndianImpl_PutUint24(t *testing.T) {
	buf := make([]byte, 3)
	BigEndian.PutUint24(buf, 795192)
	assert.EqualValues(t, []byte{12, 34, 56}, buf)
}

func TestBigEndianImpl_PutUint32(t *testing.T) {
	buf := make([]byte, 4)
	BigEndian.PutUint32(buf, 203569230)
	assert.EqualValues(t, []byte{12, 34, 56, 78}, buf)
}

func TestBigEndianImpl_PutUint64(t *testing.T) {
	buf := make([]byte, 8)
	BigEndian.PutUint64(buf, 874323186832646712)
	assert.EqualValues(t, []byte{12, 34, 56, 78, 90, 12, 34, 56}, buf)
}

func TestBigEndianImpl_String(t *testing.T) {
	assert.NotEmpty(t, BigEndian.String())
}
