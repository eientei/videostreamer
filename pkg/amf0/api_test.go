package amf0

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestUnmarshalAll(t *testing.T) {
	buf := []byte{
		byte(MarkerTypedObject),
		0x00, 0x04, 'b', 'a', 'k', 'a',
		0x00, 0x03, 'h', 'i', 'j',
		byte(MarkerBoolean),
		0x01,
		0x00, 0x03, 'j', 'k', 'l',
		byte(MarkerDate),
		0x40, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00,
		0x00, 0x03, 'q', 'w', '1',
		byte(MarkerECMAArray),
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'z', 'z', 'z',
		byte(MarkerNull),
		0x00, 0x03, 'q', 'w', '2',
		byte(MarkerLongString),
		0x00, 0x00, 0x00, 0x01, 's',
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x03, 'j', 'k', '4',
		byte(MarkerNumber),
		0x40, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x03, 'e', 'f', 'g',
		byte(MarkerObject),
		0x00, 0x03, 'g', 'g', 'g',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
		0x00, 0x03, 'q', 'w', '3',
		byte(MarkerReference),
		0x00, 0x06,
		0x00, 0x03, 'q', 'w', '4',
		byte(MarkerStrictArray),
		0x00, 0x00, 0x00, 0x01,
		byte(MarkerNull),
		0x00, 0x03, 'q', 'w', '5',
		byte(MarkerString),
		0x00, 0x01, 'z',
		0x00, 0x03, 'q', 'w', '6',
		byte(MarkerUndefined),
		0x00, 0x03, 'q', 'w', '7',
		byte(MarkerXMLDocument),
		0x00, 0x00, 0x00, 0x01, 'q',
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}
	vals, err := UnmarshalAllBytes(buf)
	assert.NoError(t, err)
	assert.Len(t, vals, 1)
	assert.EqualValues(t, &ValueTypedObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
			"efg": &ValueObject{
				Data: map[string]Value{
					"ggg": &ValueNull{},
				},
			},
			"hij": &ValueBoolean{
				Data: true,
			},
			"jk4": &ValueNumber{
				Data: 34,
			},
			"jkl": &ValueDate{
				Data: time.Unix(34, 0),
			},
			"qw1": &ValueECMAArray{
				Data: []*Property{
					{
						Key:   "zzz",
						Value: &ValueNull{},
					},
				},
			},
			"qw2": &ValueLongString{
				Data: "s",
			},
			"qw3": &ValueReference{
				Data: 6,
			},
			"qw4": &ValueStrictArray{
				Data: []Value{
					&ValueNull{},
				},
			},
			"qw5": &ValueString{
				Data: "z",
			},
			"qw6": &ValueUndefined{},
			"qw7": &ValueXMLDocument{
				Data: "q",
			},
		},
		Name: "baka",
	}, vals[0])
}

func TestMarshalAll(t *testing.T) {
	orig := &ValueTypedObject{
		Data: map[string]Value{
			"abc": &ValueNull{},
			"efg": &ValueObject{
				Data: map[string]Value{
					"ggg": &ValueNull{},
				},
			},
			"hij": &ValueBoolean{
				Data: true,
			},
			"jk4": &ValueNumber{
				Data: 34,
			},
			"jkl": &ValueDate{
				Data: time.Unix(34, 0),
			},
			"qw1": &ValueECMAArray{
				Data: []*Property{
					{
						Key:   "zzz",
						Value: &ValueNull{},
					},
				},
			},
			"qw2": &ValueLongString{
				Data: "s",
			},
			"qw3": &ValueReference{
				Data: 6,
			},
			"qw4": &ValueStrictArray{
				Data: []Value{
					&ValueNull{},
				},
			},
			"qw5": &ValueString{
				Data: "z",
			},
			"qw6": &ValueUndefined{},
			"qw7": &ValueXMLDocument{
				Data: "q",
			},
		},
		Name: "baka",
	}
	data := []byte{
		byte(MarkerTypedObject),
		0x00, 0x04, 'b', 'a', 'k', 'a',
		0x00, 0x03, 'h', 'i', 'j',
		byte(MarkerBoolean),
		0x01,
		0x00, 0x03, 'j', 'k', 'l',
		byte(MarkerDate),
		0x40, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00,
		0x00, 0x03, 'q', 'w', '1',
		byte(MarkerECMAArray),
		0x00, 0x00, 0x00, 0x01,
		0x00, 0x03, 'z', 'z', 'z',
		byte(MarkerNull),
		0x00, 0x03, 'q', 'w', '2',
		byte(MarkerLongString),
		0x00, 0x00, 0x00, 0x01, 's',
		0x00, 0x03, 'a', 'b', 'c',
		byte(MarkerNull),
		0x00, 0x03, 'j', 'k', '4',
		byte(MarkerNumber),
		0x40, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x03, 'e', 'f', 'g',
		byte(MarkerObject),
		0x00, 0x03, 'g', 'g', 'g',
		byte(MarkerNull),
		0x00, 0x00,
		byte(MarkerObjectEnd),
		0x00, 0x03, 'q', 'w', '3',
		byte(MarkerReference),
		0x00, 0x06,
		0x00, 0x03, 'q', 'w', '4',
		byte(MarkerStrictArray),
		0x00, 0x00, 0x00, 0x01,
		byte(MarkerNull),
		0x00, 0x03, 'q', 'w', '5',
		byte(MarkerString),
		0x00, 0x01, 'z',
		0x00, 0x03, 'q', 'w', '6',
		byte(MarkerUndefined),
		0x00, 0x03, 'q', 'w', '7',
		byte(MarkerXMLDocument),
		0x00, 0x00, 0x00, 0x01, 'q',
		0x00, 0x00,
		byte(MarkerObjectEnd),
	}
	bs, err := MarshalAllBytes(orig)
	assert.NoError(t, err)
	assert.Len(t, bs, len(data))
	val, err := UnmarshalAllBytes(data)
	assert.NoError(t, err)
	assert.EqualValues(t, val[0].String(), orig.String())
}
