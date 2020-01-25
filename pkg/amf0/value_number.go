package amf0

import (
	"io"
	"math"
	"strconv"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// ValueNumber represents AMF0 float64 number value
type ValueNumber struct {
	Data float64
}

func (value *ValueNumber) Read(reader io.Reader) error {
	buf := make([]byte, 8)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	value.Data = math.Float64frombits(byteorder.BigEndian.Uint64(buf))

	return nil
}

func (value *ValueNumber) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerNumber)})
	if err != nil {
		return err
	}

	buf := make([]byte, 8)

	byteorder.BigEndian.PutUint64(buf, math.Float64bits(value.Data))

	_, err = writer.Write(buf)

	return err
}

func (value *ValueNumber) String() string {
	return strconv.FormatFloat(value.Data, 'g', -1, 64)
}
