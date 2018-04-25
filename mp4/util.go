package mp4

import "io"

func Write8(writer io.Writer, value uint8) {
	writer.Write([]byte{value})
}

func Write16(writer io.Writer, value uint16) {
	writer.Write([]byte{byte(value >> 8), byte(value)})
}

func Write24(writer io.Writer, value uint32) {
	writer.Write([]byte{byte(value >> 16), byte(value >> 8), byte(value)})
}

func Write32(writer io.Writer, value uint32) {
	writer.Write([]byte{byte(value >> 24), byte(value >> 16), byte(value >> 8), byte(value)})
}

func Write64(writer io.Writer, value uint64) {
	writer.Write([]byte{byte(value >> 56), byte(value >> 48), byte(value >> 40), byte(value >> 32), byte(value >> 24), byte(value >> 16), byte(value >> 8), byte(value)})
}

func WriteB32(data []byte, value uint32) {
	data[0] = byte(value >> 24)
	data[1] = byte(value >> 16)
	data[2] = byte(value >> 8)
	data[3] = byte(value)
}

func ReadB16(data []byte) uint16 {
	return uint16(data[0])<<8 | uint16(data[1])
}

func ReadB24(data []byte) uint32 {
	return uint32(data[0])<<16 | uint32(data[1])<<8 | uint32(data[2])
}

func ReadB32(data []byte) uint32 {
	return uint32(data[0])<<24 | uint32(data[1])<<16 | uint32(data[2])<<8 | uint32(data[3])
}
