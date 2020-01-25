// Package byteorder provides conversion to and from integers of different lengths and their byte-form representations
package byteorder

import "encoding/binary"

// ByteOrder provides method for conversion to and from integers of different lengths and
// their byte-form representations
type ByteOrder interface {
	binary.ByteOrder
	Uint24([]byte) uint32
	PutUint24([]byte, uint32)
}

// BigEndian works with big-endian byte order
var BigEndian ByteOrder = &BigEndianImpl{}

// LittleEndian works with little-endian byte order
var LittleEndian ByteOrder = &LittleEndianImpl{}
