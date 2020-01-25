package amf0

import (
	"io"
	"math"
	"time"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// ValueDate represents AMF0 date time value
type ValueDate struct {
	Data time.Time
}

func (value *ValueDate) Read(reader io.Reader) error {
	buf := make([]byte, 8)

	_, err := io.ReadFull(reader, buf)
	if err != nil {
		return err
	}

	ms := math.Float64frombits(byteorder.BigEndian.Uint64(buf))

	// skip TZ
	_, err = io.ReadFull(reader, make([]byte, 2))
	if err != nil {
		return err
	}

	value.Data = time.Unix(int64(ms), 0)

	return nil
}

func (value *ValueDate) Write(writer io.Writer) error {
	_, err := writer.Write([]byte{byte(MarkerDate)})
	if err != nil {
		return err
	}

	buf := make([]byte, 8)

	byteorder.BigEndian.PutUint64(buf, math.Float64bits(float64(value.Data.Unix())))

	_, err = writer.Write(buf)
	if err != nil {
		return err
	}

	buf = make([]byte, 2)

	byteorder.BigEndian.PutUint16(buf, 0)

	_, err = writer.Write(buf)

	return err
}

func (value *ValueDate) String() string {
	return value.Data.Format(time.RFC3339)
}
