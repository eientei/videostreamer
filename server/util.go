package server

import (
	"encoding/binary"
	"io"
)

func BUint32(data []byte) uint32 {
	return binary.BigEndian.Uint32(data)
}

func BUint24(data []byte) uint32 {
	return (uint32(data[0]) << 16) | (uint32(data[1]) << 8) | uint32(data[2])
}

func LUint24(data []byte) uint32 {
	return (uint32(data[2]) << 16) | (uint32(data[1]) << 8) | uint32(data[0])
}

func PutBUint32(data []byte, value uint32) {
	binary.BigEndian.PutUint32(data, value)
}

func PutBUint24(data []byte, value uint32) {
	data[0] = byte((value >> 16) & 0xFF)
	data[1] = byte((value >> 8) & 0xFF)
	data[2] = byte(value & 0xFF)
}

func PutLUint24(data []byte, value uint32) {
	data[0] = byte(value & 0xFF)
	data[1] = byte((value >> 8) & 0xFF)
	data[2] = byte((value >> 16) & 0xFF)
}

func Write8(data io.Writer, value uint8) {
	data.Write([]byte{value})
}

func Write16(data io.Writer, value uint16) {
	data.Write([]byte{uint8((value >> 8) & 0xFF), uint8(value & 0xFF)})
}

func Write24(data io.Writer, value uint32) {
	data.Write([]byte{uint8((value >> 16) & 0xFF), uint8((value >> 8) & 0xFF), uint8(value & 0xFF)})
}

func Write32(data io.Writer, value uint32) {
	data.Write([]byte{uint8((value >> 24) & 0xFF), uint8((value >> 16) & 0xFF), uint8((value >> 8) & 0xFF), uint8(value & 0xFF)})
}

func Write64(data io.Writer, value uint64) {
	data.Write([]byte{
		uint8((value >> 56) & 0xFF),
		uint8((value >> 48) & 0xFF),
		uint8((value >> 40) & 0xFF),
		uint8((value >> 32) & 0xFF),
		uint8((value >> 24) & 0xFF),
		uint8((value >> 16) & 0xFF),
		uint8((value >> 8) & 0xFF),
		uint8(value & 0xFF),
	})
}