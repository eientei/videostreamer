package server

import (
	"bytes"
	"errors"
	"io"
	"log"
	"math"

	"github.com/eientei/videostreamer/amf"
	"github.com/eientei/videostreamer/mp4"
	"github.com/eientei/videostreamer/util"
)

var UnknownCodec = errors.New("unknown codec")

const tvid = 1000

const presentoff = 0 +
	8 + // sidx
	4 + // sidx version and flags
	4 + // sidx refid
	4 // sidx timescale
const sequenceoff = 0 +
	8 + // moof
	8 + // mfhd
	4 // mfhd version and flags
const timeoff = 0 +
	sequenceoff +
	4 + // sequence
	8 + // traf
	8 + // tfhd
	4 + // tfhd version and flags
	4 + // tfhd trackid
	4 + // tfhd flags
	8 + // tfdt
	4 // tfdt version and flags

const (
	VideoAVCCodec         = 7
	VideoInfoCommandFrame = 5
	Video                 = 1
	Audio                 = 2
)

type Nalu struct {
	Type     uint8
	FrameNum uint32
	Data     []byte
}

type Client struct {
	Conn           io.WriteCloser
	Initialized    bool
	Booted         bool
	FirstVCL       bool
	Sequence       uint32
	AudioStartTime uint64
	VideoStartTime uint64
}

type Segment struct {
	Index     []byte
	Data      []byte
	Type      uint8
	Timestamp uint32
}

type Stream struct {
	Name          string
	Logger        *log.Logger
	ContainerInit []byte // (ftyp moov)
	CodecInit     []byte // (moof mdat) with NALU 6
	Clients       []*Client

	Height    uint32
	Width     uint32
	FrameRate uint32
	AudioRate uint32

	LengthSize   uint32
	FrameNumBits uint32

	SkipToKeyframe bool
	Buffer         []*Segment
	PrevFrameNum   uint32
}

type Context struct {
	Streams map[string]*Stream
}

func (stream *Stream) Close() {
	for _, client := range stream.Clients {
		client.Conn.Close()
	}
}

func (stream *Stream) Audio(data []byte, timestamp uint32) error {
	ptr := 0
	format := data[ptr] >> 4
	ptr += 1
	if format != 10 {
		return UnknownCodec
	}
	ptr += 1
	return stream.SendAudio(data[ptr:], timestamp)
}

func (stream *Stream) Video(data []byte, timestamp uint32) error {
	ptr := 0
	frame := (data[ptr] >> 4) & 0xF
	codec := data[ptr] & 0xF
	ptr += 1
	avctype := -1
	if codec == VideoAVCCodec {
		avctype = int(data[ptr])
		ptr += 1
		ptr += 3
	}
	if frame == VideoInfoCommandFrame {
		return nil
	} else {
		switch codec {
		case VideoAVCCodec:
			switch avctype {
			case 0:
				return stream.InitContainer(data[ptr:])
			case 1:
				if stream.CodecInit == nil {
					return stream.TryInitCodec(data[ptr:])
				} else {
					return stream.SendVideo(data[ptr:], timestamp)
				}
			default:
				return UnknownCodec
			}
		default:
			return UnknownCodec
		}
	}
}

func (stream *Stream) Meta(data []byte) error {
	values := amf.ReadAll(bytes.NewReader(data))
	stream.Height = uint32(values[2].(*amf.ArrayValue).Data["height"].(*amf.NumberValue).Data)
	stream.Width = uint32(values[2].(*amf.ArrayValue).Data["width"].(*amf.NumberValue).Data)
	stream.FrameRate = uint32(values[2].(*amf.ArrayValue).Data["framerate"].(*amf.NumberValue).Data)
	stream.AudioRate = uint32(values[2].(*amf.ArrayValue).Data["audiosamplerate"].(*amf.NumberValue).Data)
	return nil
}

func (stream *Stream) InitContainer(avcC []byte) error {
	ftyp := &mp4.FtypBox{
		MajorBand:       "isom",
		MinorVersion:    0x200,
		CompatibleBands: []string{"isom", "iso2", "avc1", "mp41"},
	}
	moov := &mp4.MoovBox{
		BoxChildren: []mp4.Box{
			&mp4.MvhdBox{
				CreationTime:     0,
				ModificationTime: 0,
				Timescale:        1000,
				Duration:         0,
				NextTrackId:      0xFFFFFFFF,
			},
			&mp4.TrakBox{
				BoxChildren: []mp4.Box{
					&mp4.TkhdBox{
						CreationTime:     0,
						ModificationTime: 0,
						TrackId:          1,
						Duration:         0,
						Audio:            false,
						Width:            stream.Width << 16,
						Height:           stream.Height << 16,
					},
					&mp4.MdiaBox{
						BoxChildren: []mp4.Box{
							&mp4.MdhdBox{
								CreationTime:     0,
								ModificationTime: 0,
								Timescale:        stream.FrameRate,
								Duration:         0,
							},
							&mp4.HdlrBox{
								HandlerType: "vide",
								HandlerName: "Video Handler",
							},
							&mp4.MinfBox{
								BoxChildren: []mp4.Box{
									&mp4.VmhdBox{},
									&mp4.DinfBox{
										BoxChildren: []mp4.Box{
											&mp4.DrefBox{
												BoxChildren: []mp4.Box{
													&mp4.Url_Box{},
												},
											},
										},
									},
									&mp4.StblBox{
										BoxChildren: []mp4.Box{
											&mp4.StsdBox{
												BoxChildren: []mp4.Box{
													&mp4.Avc1Box{
														DataReferenceIndex: 1,
														Width:              uint16(stream.Width),
														Height:             uint16(stream.Height),
														Compressorname:     "",
														AvcC: &mp4.AvcCBox{
															AvcCData: avcC,
														},
													},
												},
											},
											&mp4.SttsBox{},
											&mp4.StscBox{},
											&mp4.StszBox{},
											&mp4.StcoBox{},
										},
									},
								},
							},
						},
					},
				},
			},
			&mp4.TrakBox{
				BoxChildren: []mp4.Box{
					&mp4.TkhdBox{
						CreationTime:     0,
						ModificationTime: 0,
						TrackId:          2,
						Duration:         0,
						Audio:            true,
						Width:            0,
						Height:           0,
					},
					&mp4.MdiaBox{
						BoxChildren: []mp4.Box{
							&mp4.MdhdBox{
								CreationTime:     0,
								ModificationTime: 0,
								Timescale:        stream.AudioRate,
								Duration:         0,
							},
							&mp4.HdlrBox{
								HandlerType: "soun",
								HandlerName: "Sound Handler",
							},
							&mp4.MinfBox{
								BoxChildren: []mp4.Box{
									&mp4.SmhdBox{},
									&mp4.DinfBox{
										BoxChildren: []mp4.Box{
											&mp4.DrefBox{
												BoxChildren: []mp4.Box{
													&mp4.Url_Box{},
												},
											},
										},
									},
									&mp4.StblBox{
										BoxChildren: []mp4.Box{
											&mp4.StsdBox{
												BoxChildren: []mp4.Box{
													&mp4.Mp4aBox{
														DataReferenceIndex: 1,
														SampleRate:         stream.AudioRate,
														Esds: &mp4.EsdsBox{
															Bitrate:   0,
															Frequency: stream.AudioRate,
														},
													},
												},
											},
											&mp4.SttsBox{},
											&mp4.StscBox{},
											&mp4.StszBox{},
											&mp4.StcoBox{},
										},
									},
								},
							},
						},
					},
				},
			},
			&mp4.MvexBox{
				BoxChildren: []mp4.Box{
					&mp4.MehdBox{
						TimeScale: 1000,
					},
					&mp4.TrexBox{
						TrackId: 1,
					},
					&mp4.TrexBox{
						TrackId: 2,
					},
				},
			},
		},
	}

	stream.LengthSize = uint32(avcC[4]&3) + 1
	p := uint16(6)
	for i := byte(0); i < avcC[5]&0x1f; i++ {
		l := util.ReadB16(avcC[p : p+2])
		bitptr := uint64(p+3) * 8
		ReadExpGolomb(avcC, &bitptr) // seq parameter set id
		stream.FrameNumBits = uint32(ReadExpGolomb(avcC, &bitptr) + 4)
		p += l
	}

	buf := &bytes.Buffer{}
	buf.Write(mp4.BoxWrite(ftyp))
	buf.Write(mp4.BoxWrite(moov))
	stream.ContainerInit = buf.Bytes()

	return nil
}

func (stream *Stream) SendAudio(data []byte, timestamp uint32) error {
	moof := &mp4.MoofBox{
		BoxChildren: []mp4.Box{
			&mp4.MfhdBox{
				Sequence: 0,
			},
			&mp4.TrafBox{
				BoxChildren: []mp4.Box{
					&mp4.TfhdBox{
						TrackId: 2,
					},
					&mp4.TfdtBox{
						BaseMediaDecodeTime: 0,
					},
					&mp4.TrunBox{
						SampleSizes: []*mp4.Sample{{Duration: 1024, Size: uint32(len(data))}},
					},
				},
			},
		},
	}
	mdat := &mp4.MdatBox{
		BoxData: data,
	}

	moofdata := mp4.BoxWrite(moof)

	sidx := &mp4.SidxBox{
		ReferenceId:        2,
		Timescale:          stream.AudioRate,
		PresentationTime:   0,
		ReferenceSize:      uint32(len(moofdata)),
		SubsegmentDuration: 1024,
	}

	t1off := len(moofdata) - 8 - 4
	util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

	segment := &bytes.Buffer{}
	segment.Write(moofdata)
	segment.Write(mp4.BoxWrite(mdat))

	segmentidx := mp4.BoxWrite(sidx)
	segmentdata := segment.Bytes()

	return stream.SendSegment(segmentidx, segmentdata, false, Audio, timestamp)
}

func (stream *Stream) FormVideo(videos []*mp4.Sample, data []byte, keyframe bool, timestamp uint32) error {
	moof := &mp4.MoofBox{
		BoxChildren: []mp4.Box{
			&mp4.MfhdBox{
				Sequence: 0,
			},
			&mp4.TrafBox{
				BoxChildren: []mp4.Box{
					&mp4.TfhdBox{
						TrackId: 1,
						Flags:   0x2000000,
					},
					&mp4.TfdtBox{
						BaseMediaDecodeTime: 0,
					},
					&mp4.TrunBox{
						SampleSizes: videos,
					},
				},
			},
		},
	}
	mdat := &mp4.MdatBox{
		BoxData: data,
	}

	moofdata := mp4.BoxWrite(moof)

	sidx := &mp4.SidxBox{
		ReferenceId:        1,
		Timescale:          stream.FrameRate,
		PresentationTime:   0,
		ReferenceSize:      uint32(len(moofdata)),
		SubsegmentDuration: 1,
	}

	t1off := len(moofdata) - len(videos)*8 - 4
	util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

	segment := &bytes.Buffer{}
	segment.Write(moofdata)
	segment.Write(mp4.BoxWrite(mdat))

	segmentidx := mp4.BoxWrite(sidx)
	segmentdata := segment.Bytes()

	return stream.SendSegment(segmentidx, segmentdata, keyframe, Video, timestamp)
}

func (stream *Stream) SendVideo(data []byte, timestamp uint32) error {
	buf := &bytes.Buffer{}
	videos := make([]*mp4.Sample, 0)
	nalus := stream.BreakNals(data)
	keyframe := false
	for _, nalu := range nalus {
		if nalu.Type == 5 {
			keyframe = true
		}
		if stream.SkipToKeyframe && !keyframe {
			continue
		} else if keyframe {
			stream.SkipToKeyframe = false
		}
		if nalu.Type == 12 {
			continue
		}
		//dur := uint32(tvid / stream.FrameRate)
		/*
			if len(videos) > 0 && nalu.FrameNum != stream.PrevFrameNum {
				stream.FormVideo(videos, buf.Bytes(), keyframe)
				videos = videos[:0]
				buf = &bytes.Buffer{}
				keyframe = false
			}
		*/
		videos = append(videos, &mp4.Sample{Duration: 1, Size: uint32(len(nalu.Data))})
		buf.Write(nalu.Data)
		stream.PrevFrameNum = nalu.FrameNum
	}

	if len(videos) > 0 {
		return stream.FormVideo(videos, buf.Bytes(), keyframe, timestamp)
	} else {
		return nil
	}

}

func (stream *Stream) SendSegment(index []byte, data []byte, keyframe bool, typ uint8, timestamp uint32) error {
	if len(stream.Buffer) > 0 && keyframe {
		buf := &bytes.Buffer{}
		for _, segment := range stream.Buffer {
			buf.Write(segment.Index)
			buf.Write(segment.Data)
		}
		bufdata := buf.Bytes()
		toremove := make([]*Client, 0)
		for _, client := range stream.Clients {
			if !client.Initialized {
				if _, err := client.Conn.Write(stream.ContainerInit); err != nil {
					client.Conn.Close()
					toremove = append(toremove, client)
					continue
				}
				client.Initialized = true
			} else if !client.Booted && client.FirstVCL {
				//if _, err := client.Conn.Write(stream.CodecInit); err != nil {
				//	client.Conn.Close()
				//	toremove = append(toremove, client)
				//}
				client.Booted = true
			}
			off := 0
			wasvideo := false
			for _, segment := range stream.Buffer {
				client.Sequence++
				switch segment.Type {
				case Audio:
					util.WriteB64(bufdata[off+presentoff:off+presentoff+8], client.AudioStartTime)
				case Video:
					util.WriteB64(bufdata[off+presentoff:off+presentoff+8], client.VideoStartTime)
				}
				off += len(segment.Index)
				util.WriteB32(bufdata[off+sequenceoff:off+sequenceoff+4], client.Sequence)
				switch segment.Type {
				case Audio:
					util.WriteB64(bufdata[off+timeoff:off+timeoff+8], client.AudioStartTime)
					client.AudioStartTime += 1024
				case Video:
					util.WriteB64(bufdata[off+timeoff:off+timeoff+8], client.VideoStartTime)
					client.VideoStartTime += 1
					wasvideo = true
				}
				off += len(segment.Data)
			}
			if wasvideo && !client.FirstVCL {
				client.FirstVCL = true
			}
			if client.FirstVCL {
				if _, err := client.Conn.Write(bufdata); err != nil {
					client.Conn.Close()
					toremove = append(toremove, client)
					continue
				}
			}
		}
		stream.Buffer = stream.Buffer[:0]
		if len(toremove) > 0 {
			nclients := make([]*Client, 0)
			for _, client := range stream.Clients {
				add := true
				for _, rem := range toremove {
					if rem == client {
						add = false
						break
					}
				}
				if add {
					nclients = append(nclients, client)
				}
			}
			stream.Clients = nclients
		}
	}
	stream.Buffer = append(stream.Buffer, &Segment{Index: index, Data: data, Type: typ, Timestamp: timestamp})
	return nil
}

func (stream *Stream) TryInitCodec(data []byte) error {
	buf := &bytes.Buffer{}
	videos := make([]*mp4.Sample, 0)
	nalus := stream.BreakNals(data)
	var s *mp4.Sample
	var d []byte
	keyframe := false
	for _, nalu := range nalus {
		if nalu.Type == 6 {
			s = &mp4.Sample{Duration: 0, Size: uint32(len(nalu.Data))}
			d = nalu.Data
		}
		if nalu.Type == 5 {
			videos = append(videos, &mp4.Sample{Duration: 0, Size: uint32(len(nalu.Data))})
			buf.Write(nalu.Data)
			keyframe = true
		}
	}

	stream.FormVideo(videos, buf.Bytes(), keyframe, 0)

	if s == nil {
		return nil
	}
	moof := &mp4.MoofBox{
		BoxChildren: []mp4.Box{
			&mp4.MfhdBox{
				Sequence: 0,
			},
			&mp4.TrafBox{
				BoxChildren: []mp4.Box{
					&mp4.TfhdBox{
						TrackId: 1,
					},
					&mp4.TfdtBox{
						BaseMediaDecodeTime: 0,
					},
					&mp4.TrunBox{
						SampleSizes: []*mp4.Sample{s},
					},
				},
			},
		},
	}
	mdat := &mp4.MdatBox{
		BoxData: d,
	}

	moofdata := mp4.BoxWrite(moof)

	t1off := len(moofdata) - 8 - 4
	util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

	segment := &bytes.Buffer{}
	segment.Write(moofdata)
	segment.Write(mp4.BoxWrite(mdat))

	stream.CodecInit = segment.Bytes()
	stream.SkipToKeyframe = true
	return nil
}

func (stream *Stream) BreakNals(data []byte) []*Nalu {
	nalus := make([]*Nalu, 0)
	ptr := uint32(0)
	for {
		length := uint32(0)
		switch stream.LengthSize {
		case 1:
			length = uint32(data[ptr]) + 1
		case 2:
			length = uint32(util.ReadB16(data[ptr:ptr+2])) + 2
		case 3:
			length = uint32(util.ReadB24(data[ptr:ptr+3])) + 3
		case 4:
			length = util.ReadB32(data[ptr:ptr+4]) + 4
		}
		p := ptr + stream.LengthSize
		nalutype := data[p] & 0x1f
		p++
		if uint32(len(data)) >= p+3 && bytes.Equal(data[p:p+3], []byte{0x00, 0x00, 0x03}) {
			p += 3
		} else {
			p += 1
		}
		bitptr := uint64(p * 8)
		ReadExpGolomb(data, &bitptr) // firstmb
		ReadExpGolomb(data, &bitptr) // slicetype
		ReadExpGolomb(data, &bitptr) // ppsid
		frameNum := uint32(ReadBits(data, &bitptr, uint64(stream.FrameNumBits)))
		nalus = append(nalus, &Nalu{nalutype, frameNum, data[ptr : ptr+length]})
		ptr += length
		if ptr >= uint32(len(data)) {
			break
		}
	}
	return nalus
}

func ReadBits(data []byte, bitptr *uint64, nbits uint64) uint64 {
	ptr := *bitptr / 8
	est := (*bitptr + nbits) / 8
	if (*bitptr+nbits)%8 > 0 {
		est += 1
	}
	if est > uint64(len(data)) {
		return ^uint64(0)
	}
	res := uint64(0)
	pre := *bitptr % 8
	post := pre + nbits
	if post > 8 {
		post = 8
	}
	sub := data[ptr:est]
	for i, b := range sub {
		lm := post - pre
		mask := uint64(0)
		for n := uint64(0); n < lm; n++ {
			mask |= 1 << n
		}
		res |= uint64(b>>pre) & mask
		if i < len(sub)-1 {
			res <<= lm
		}
	}
	*bitptr += nbits
	return res
}

func ReadExpGolomb(data []byte, bitptr *uint64) uint64 {
	lzero := float64(-1)
	for b := uint64(0); b == 0; lzero++ {
		b = ReadBits(data, bitptr, 1)
	}
	return uint64(math.Pow(2, lzero)) - 1 + ReadBits(data, bitptr, uint64(lzero))
}
