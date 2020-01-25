package amf0

// Marker type for AMF0 entities
type Marker byte

// Known markers
const (
	MarkerNumber      Marker = 0x00
	MarkerBoolean     Marker = 0x01
	MarkerString      Marker = 0x02
	MarkerObject      Marker = 0x03
	MarkerMovieclip   Marker = 0x04 // not supported by spec
	MarkerNull        Marker = 0x05
	MarkerUndefined   Marker = 0x06
	MarkerReference   Marker = 0x07
	MarkerECMAArray   Marker = 0x08
	MarkerObjectEnd   Marker = 0x09
	MarkerStrictArray Marker = 0x0A
	MarkerDate        Marker = 0x0B
	MarkerLongString  Marker = 0x0C
	MarkerUnsupported Marker = 0x0D
	MarkerRecordset   Marker = 0x0E // not supported by spec
	MarkerXMLDocument Marker = 0x0F
	MarkerTypedObject Marker = 0x10
	MarkerAMF3        Marker = 0x11 // not supported by implementation
	MarkerInvalid     Marker = 0xFF
)
