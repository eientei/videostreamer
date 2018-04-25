package amf

func ReadB16(data []byte) uint16 {
	return uint16(data[0])<<8 | uint16(data[1])
}

func ReadB64(data []byte) uint64 {
	return uint64(data[0])<<56 | uint64(data[1])<<48 | uint64(data[2])<<40 | uint64(data[3])<<32 | uint64(data[4])<<24 | uint64(data[5])<<16 | uint64(data[6])<<8 | uint64(data[7])
}

func WriteB16(data []byte, value uint16) {
	data[0] = byte(value >> 8)
	data[1] = byte(value)
}

func WriteB32(data []byte, value uint32) {
	data[0] = byte(value >> 24)
	data[1] = byte(value >> 16)
	data[2] = byte(value >> 8)
	data[3] = byte(value)
}

func WriteB64(data []byte, value uint64) {
	data[0] = byte(value >> 56)
	data[1] = byte(value >> 48)
	data[2] = byte(value >> 40)
	data[3] = byte(value >> 32)
	data[4] = byte(value >> 24)
	data[5] = byte(value >> 16)
	data[6] = byte(value >> 8)
	data[7] = byte(value)
}
