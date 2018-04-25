package rtmp

func ReadB24(data []byte) uint32 {
	return uint32(data[0])<<16 | uint32(data[1])<<8 | uint32(data[2])
}

func ReadB32(data []byte) uint32 {
	return uint32(data[0])<<24 | uint32(data[1])<<16 | uint32(data[2])<<8 | uint32(data[3])
}

func ReadL32(data []byte) uint32 {
	return uint32(data[3])<<24 | uint32(data[2])<<16 | uint32(data[1])<<8 | uint32(data[0])
}

func WriteB16(data []byte, value uint16) {
	data[0] = byte(value >> 8)
	data[1] = byte(value)
}

func WriteB24(data []byte, value uint32) {
	data[0] = byte(value >> 16)
	data[1] = byte(value >> 8)
	data[2] = byte(value)
}

func WriteB32(data []byte, value uint32) {
	data[0] = byte(value >> 24)
	data[1] = byte(value >> 16)
	data[2] = byte(value >> 8)
	data[3] = byte(value)
}

func WriteL32(data []byte, value uint32) {
	data[3] = byte(value >> 24)
	data[2] = byte(value >> 16)
	data[1] = byte(value >> 8)
	data[0] = byte(value)
}
