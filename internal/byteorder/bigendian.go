package byteorder

// BigEndianImpl implements big-endian byte order
type BigEndianImpl struct {
}

// Uint16 loads 16-bit unsigned integer from byte representation
func (impl *BigEndianImpl) Uint16(b []byte) uint16 {
	_ = b[1]
	return uint16(b[1]) | uint16(b[0])<<8
}

// PutUint16 stores 16-bit unsigned integer to byte representation
func (impl *BigEndianImpl) PutUint16(b []byte, v uint16) {
	_ = b[1]
	b[0] = byte(v >> 8)
	b[1] = byte(v)
}

// Uint24 loads 24-bit unsigned integer from byte representation
func (impl *BigEndianImpl) Uint24(b []byte) uint32 {
	_ = b[2]
	return uint32(b[2]) | uint32(b[1])<<8 | uint32(b[0])<<16
}

// PutUint24 stores 24-bit unsigned integer to byte representation
func (impl *BigEndianImpl) PutUint24(b []byte, v uint32) {
	_ = b[2]
	b[0] = byte(v >> 16)
	b[1] = byte(v >> 8)
	b[2] = byte(v)
}

// Uint32 loads 32-bit unsigned integer from byte representation
func (impl *BigEndianImpl) Uint32(b []byte) uint32 {
	_ = b[3]
	return uint32(b[3]) | uint32(b[2])<<8 | uint32(b[1])<<16 | uint32(b[0])<<24
}

// PutUint32 stores 32-bit unsigned integer to byte representation
func (impl *BigEndianImpl) PutUint32(b []byte, v uint32) {
	_ = b[3]
	b[0] = byte(v >> 24)
	b[1] = byte(v >> 16)
	b[2] = byte(v >> 8)
	b[3] = byte(v)
}

// Uint64 loads 64-bit unsigned integer from byte representation
func (impl *BigEndianImpl) Uint64(b []byte) uint64 {
	_ = b[7]

	return uint64(b[7]) |
		uint64(b[6])<<8 |
		uint64(b[5])<<16 |
		uint64(b[4])<<24 |
		uint64(b[3])<<32 |
		uint64(b[2])<<40 |
		uint64(b[1])<<48 |
		uint64(b[0])<<56
}

// PutUint64 stores 64-bit unsigned integer to byte representation
func (impl *BigEndianImpl) PutUint64(b []byte, v uint64) {
	_ = b[7]
	b[0] = byte(v >> 56)
	b[1] = byte(v >> 48)
	b[2] = byte(v >> 40)
	b[3] = byte(v >> 32)
	b[4] = byte(v >> 24)
	b[5] = byte(v >> 16)
	b[6] = byte(v >> 8)
	b[7] = byte(v)
}

// String implements stringer interface
func (impl *BigEndianImpl) String() string {
	return "BigEndian"
}
