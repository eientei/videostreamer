package util

import "io"

func ReadB16(data []byte) uint16 {
	return uint16(data[0])<<8 | uint16(data[1])
}

func ReadB24(data []byte) uint32 {
	return uint32(data[0])<<16 | uint32(data[1])<<8 | uint32(data[2])
}

func ReadB32(data []byte) uint32 {
	return uint32(data[0])<<24 | uint32(data[1])<<16 | uint32(data[2])<<8 | uint32(data[3])
}

func ReadB64(data []byte) uint64 {
	return uint64(data[0])<<56 | uint64(data[1])<<48 | uint64(data[2])<<40 | uint64(data[3])<<32 | uint64(data[4])<<24 | uint64(data[5])<<16 | uint64(data[6])<<8 | uint64(data[7])
}

func ReadL16(data []byte) uint16 {
	return uint16(data[1])<<8 | uint16(data[0])
}

func ReadL24(data []byte) uint32 {
	return uint32(data[2])<<16 | uint32(data[1])<<8 | uint32(data[0])
}

func ReadL32(data []byte) uint32 {
	return uint32(data[3])<<24 | uint32(data[2])<<16 | uint32(data[1])<<8 | uint32(data[0])
}

func ReadL64(data []byte) uint64 {
	return uint64(data[7])<<56 | uint64(data[6])<<48 | uint64(data[5])<<40 | uint64(data[4])<<32 | uint64(data[3])<<24 | uint64(data[2])<<16 | uint64(data[1])<<8 | uint64(data[0])
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

func WriteL16(data []byte, value uint16) {
	data[1] = byte(value >> 8)
	data[0] = byte(value)
}

func WriteL24(data []byte, value uint32) {
	data[2] = byte(value >> 16)
	data[1] = byte(value >> 8)
	data[0] = byte(value)
}

func WriteL32(data []byte, value uint32) {
	data[3] = byte(value >> 24)
	data[2] = byte(value >> 16)
	data[1] = byte(value >> 8)
	data[0] = byte(value)
}

func WriteL64(data []byte, value uint64) {
	data[7] = byte(value >> 56)
	data[6] = byte(value >> 48)
	data[5] = byte(value >> 40)
	data[4] = byte(value >> 32)
	data[3] = byte(value >> 24)
	data[2] = byte(value >> 16)
	data[1] = byte(value >> 8)
	data[0] = byte(value)
}

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
