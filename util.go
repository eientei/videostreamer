package main

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
