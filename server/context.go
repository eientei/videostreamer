package server

import (
	"bytes"
	"errors"
	"io"
	"log"

	"../amf"
	"../mp4"
	"../util"
)

var InvalidProfile = errors.New("invalid profile")
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
	Reader *NaluReader
	Data   []byte
}

type Client struct {
	Conn           io.WriteCloser
	Initialized    bool
	InitFrame      []byte
	Booted         bool
	FirstVCL       bool
	Sequence       uint32
	AudioStartTime uint64
	VideoStartTime uint64
}

type Segment struct {
	Samples   []*mp4.Sample
	Data      []byte
	SliceType uint64
}

type Stream struct {
	AudioIn chan []byte
	VideoIn chan []byte

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
	FrameNumBits uint64
	ColorPlanes  bool

	SkipToKeyframe bool
	AudioBuffer    []*Segment
	VideoBuffer    []*Segment
}

type Context struct {
	Streams map[string]*Stream
}

func (stream *Stream) Close() {
	close(stream.AudioIn)
	close(stream.VideoIn)
	for _, client := range stream.Clients {
		client.Conn.Close()
	}
}

func (stream *Stream) Run() {
	for {
		select {
		case msg, ok := <-stream.AudioIn:
			if !ok {
				return
			}
			stream.Audio(msg)
		case msg, ok := <-stream.VideoIn:
			if !ok {
				return
			}
			stream.Video(msg)
		}
	}
}

func (stream *Stream) Audio(data []byte) error {
	ptr := 0
	format := data[ptr] >> 4
	ptr += 1
	if format != 10 {
		return UnknownCodec
	}
	ptr += 1
	return stream.SendAudio(data[ptr:])
}

func (stream *Stream) Video(data []byte) error {
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
				stream.SkipToKeyframe = true
				return stream.InitContainer(data[ptr:])
			case 1:
				return stream.SendVideo(data[ptr:])
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
		MajorBand:       "iso5",
		MinorVersion:    1,
		CompatibleBands: []string{"avc1", "iso5", "dash"},
	}
	moov := &mp4.MoovBox{
		BoxChildren: []mp4.Box{
			&mp4.MvhdBox{
				CreationTime:     0,
				ModificationTime: 0,
				Timescale:        1,
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

	if avcC[1] != 66 {
		return InvalidProfile
	}

	stream.LengthSize = uint32(avcC[4]&3) + 1

	b := &BitReader{Reader: bytes.NewReader(avcC[8:])}
	profileIdc := b.Read(8)
	b.Read(8) // flags
	b.Read(8) // levelIdc
	if profileIdc == 100 ||
		profileIdc == 110 ||
		profileIdc == 122 ||
		profileIdc == 244 ||
		profileIdc == 44 ||
		profileIdc == 83 ||
		profileIdc == 86 ||
		profileIdc == 118 ||
		profileIdc == 128 ||
		profileIdc == 138 ||
		profileIdc == 139 ||
		profileIdc == 134 ||
		profileIdc == 135 {
		chromeIdc := ReadExpGolomb(b)
		mat := 8
		if chromeIdc == 3 {
			if b.Read(1) == 1 {
				stream.ColorPlanes = true
			}
			mat = 12
		}
		ReadExpGolomb(b)
		ReadExpGolomb(b)
		b.Read(1)
		scale := b.Read(1)
		if scale == 1 {
			for i := 0; i < mat; i++ {
				b.Read(1)
			}
		}
	}

	stream.FrameNumBits = ReadExpGolomb(b) + 4

	buf := &bytes.Buffer{}
	buf.Write(mp4.BoxWrite(ftyp))
	buf.Write(mp4.BoxWrite(moov))
	stream.ContainerInit = buf.Bytes()

	return nil
}

func (stream *Stream) SendAudio(data []byte) error {
	/*
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
			Keyframe:           true,
		}

		t1off := len(moofdata) - 8 - 4
		util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

		segment := &bytes.Buffer{}
		segment.Write(moofdata)
		segment.Write(mp4.BoxWrite(mdat))

		segmentidx := mp4.BoxWrite(sidx)
		segmentdata := segment.Bytes()
	*/
	copydata := make([]byte, len(data))
	copy(copydata, data)
	return stream.AddSegment([]*mp4.Sample{{Duration: 1024, Size: uint32(len(data))}}, copydata, Audio, 0)
}

/*
func (stream *Stream) FormVideo(videos []*mp4.Sample, data []byte, keyframe bool) error {
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
			Keyframe:           keyframe,
		}

		t1off := len(moofdata) - len(videos)*8 - 4
		util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

		segment := &bytes.Buffer{}
		segment.Write(moofdata)
		segment.Write(mp4.BoxWrite(mdat))

		segmentidx := mp4.BoxWrite(sidx)
		segmentdata := segment.Bytes()

	return stream.AddSegment(videos, data, Video, keyframe)
}
*/
func (stream *Stream) SendVideo(data []byte) error {
	videos := make([]*mp4.Sample, 0)

	nalus := stream.BreakNals(data)

	segd := make([]byte, 0)

	lframenum := uint64(0)

	//lnaltyp := uint64(0)
	lstyp := uint64(0)

	for _, nalu := range nalus {
		if nalu.Reader.Type != 1 && nalu.Reader.Type != 5 {
			continue
		}

		ReadExpGolomb(nalu.Reader.Bitr)
		nextslice := ReadExpGolomb(nalu.Reader.Bitr)
		ReadExpGolomb(nalu.Reader.Bitr) // pps id
		if stream.ColorPlanes {
			nalu.Reader.Bitr.Read(2)
		}
		nextframe := nalu.Reader.Bitr.Read(stream.FrameNumBits)

		if stream.SkipToKeyframe {
			if nalu.Reader.Type != 5 {
				continue
			}
			stream.SkipToKeyframe = false
		}

		if len(segd) > 0 {
			if lframenum != nextframe {
				stream.AddSegment(videos, segd, Video, lstyp)

				videos = videos[:0]
				segd = segd[:0]
			}
		}

		videos = append(videos, &mp4.Sample{Duration: 1, Size: uint32(len(nalu.Data))})
		segd = append(segd, nalu.Data...)
		//lnaltyp = nalu.Reader.Type
		lstyp = nextslice
		lframenum = nextframe
	}

	if len(segd) > 0 {
		return stream.AddSegment(videos, segd, Video, lstyp)
	}
	return nil
}

func (stream *Stream) SendSegment(viddata []byte, vsidx int, vsamp int, auddata []byte, asidx int, asamp int) error {
	toremove := make([]*Client, 0)
	for _, client := range stream.Clients {
		voff := 0
		aoff := 0
		util.WriteB64(viddata[presentoff:presentoff+8], client.VideoStartTime)
		util.WriteB64(auddata[presentoff:presentoff+8], client.AudioStartTime)
		voff += vsidx
		aoff += asidx

		util.WriteB32(viddata[voff+sequenceoff:voff+sequenceoff+4], client.Sequence)
		client.Sequence++
		util.WriteB32(auddata[aoff+sequenceoff:aoff+sequenceoff+4], client.Sequence)
		client.Sequence++

		util.WriteB64(viddata[voff+timeoff:voff+timeoff+8], client.VideoStartTime)
		client.VideoStartTime += uint64(1 * vsamp)

		util.WriteB64(auddata[aoff+timeoff:aoff+timeoff+8], client.AudioStartTime)
		client.AudioStartTime += uint64(1024 * asamp)

		if _, err := client.Conn.Write(append(viddata, auddata...)); err != nil {
			client.Conn.Close()
			toremove = append(toremove, client)
			continue
		}
	}

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
	return nil
}

func (stream *Stream) AddSegment(newsamples []*mp4.Sample, sampledata []byte, typ uint8, slicetyp uint64) error {
	asegment := &bytes.Buffer{}
	vsegment := &bytes.Buffer{}

	asidx := 0
	vsidx := 0

	asamp := 0
	vsamp := 0

	if len(stream.AudioBuffer) > 0 && slicetyp == 7 {
		databuf := &bytes.Buffer{}
		samples := make([]*mp4.Sample, 0)
		for _, seg := range stream.AudioBuffer {
			samples = append(samples, seg.Samples...)
			databuf.Write(seg.Data)
		}

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
							Samples: samples,
						},
					},
				},
			},
		}

		moofdata := mp4.BoxWrite(moof)

		mdat := &mp4.MdatBox{
			BoxData: databuf.Bytes(),
		}

		mdata := mp4.BoxWrite(mdat)

		sidx := &mp4.SidxBox{
			ReferenceId:        2,
			Timescale:          stream.AudioRate,
			PresentationTime:   0,
			ReferenceSize:      uint32(len(moofdata)) + uint32(len(mdata)),
			SubsegmentDuration: 1024 * uint32(len(samples)),
			Keyframe:           true,
		}

		t1off := len(moofdata) - len(samples)*12 - 4
		util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

		sidxdata := mp4.BoxWrite(sidx)

		asegment.Write(sidxdata)
		asegment.Write(moofdata)
		asegment.Write(mdata)

		asidx = len(sidxdata)
		asamp = len(samples)
		stream.AudioBuffer = stream.AudioBuffer[:0]
	}

	if len(stream.VideoBuffer) > 0 && slicetyp == 7 {
		keyframe := false
		databuf := &bytes.Buffer{}
		samples := make([]*mp4.Sample, 0)
		for i, seg := range stream.VideoBuffer {
			pts := i
			if i == 0 && seg.SliceType == 7 {
				keyframe = true
			}
			if seg.SliceType == 5 || seg.SliceType == 7 {
				M := 0
				for n := i + 1; n < len(stream.VideoBuffer); n++ {
					if stream.VideoBuffer[n].SliceType == 5 || stream.VideoBuffer[n].SliceType == 7 {
						break
					}
					if stream.VideoBuffer[n].SliceType == 6 {
						M++
					}
				}
				pts += M + 1
			}

			for x, sample := range seg.Samples {
				if x > 0 {
					//sample.Duration = 0
				}
				sample.Scto = uint32(pts - i)
				samples = append(samples, sample)
			}
			databuf.Write(seg.Data)
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
							Samples: samples,
						},
					},
				},
			},
		}

		moofdata := mp4.BoxWrite(moof)

		mdat := &mp4.MdatBox{
			BoxData: databuf.Bytes(),
		}

		mdata := mp4.BoxWrite(mdat)

		sidx := &mp4.SidxBox{
			ReferenceId:        1,
			Timescale:          stream.FrameRate,
			PresentationTime:   0,
			ReferenceSize:      uint32(len(moofdata)) + uint32(len(mdata)),
			SubsegmentDuration: 1 * uint32(len(samples)),
			Keyframe:           keyframe,
		}

		t1off := len(moofdata) - len(samples)*12 - 4
		util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8))

		sidxdata := mp4.BoxWrite(sidx)

		vsegment.Write(sidxdata)
		vsegment.Write(moofdata)
		vsegment.Write(mdata)

		vsidx = len(sidxdata)
		vsamp = len(samples)
		stream.VideoBuffer = stream.VideoBuffer[:0]
	}
	switch typ {
	case Audio:
		stream.AudioBuffer = append(stream.AudioBuffer, &Segment{Samples: newsamples, Data: sampledata, SliceType: slicetyp})
	case Video:
		stream.VideoBuffer = append(stream.VideoBuffer, &Segment{Samples: newsamples, Data: sampledata, SliceType: slicetyp})
	}

	if vsegment.Len() > 0 && asegment.Len() > 0 {
		stream.SendSegment(vsegment.Bytes(), vsidx, vsamp, asegment.Bytes(), asidx, asamp)
	}
	return nil
}

/*
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

	stream.FormVideo(videos, buf.Bytes(), keyframe)

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
*/
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
		r := MakeNaluReader(data[ptr+4 : ptr+length])
		nalus = append(nalus, &Nalu{Reader: r, Data: data[ptr : ptr+length]})
		ptr += length
		if ptr >= uint32(len(data)) {
			break
		}
	}
	return nalus
}

type NaluReader struct {
	Data   []byte
	Ptr    int
	Bitr   *BitReader
	RefIdc uint64
	Type   uint64
	Rbsp   bool
	Buf    []byte
}

func MakeNaluReader(data []byte) *NaluReader {
	r := &NaluReader{Data: data}
	r.Bitr = &BitReader{Reader: r}
	r.Bitr.Read(1) // zero
	r.RefIdc = r.Bitr.Read(2)
	r.Type = r.Bitr.Read(5)
	if r.Type == 14 || r.Type == 20 || r.Type == 21 {
		return nil
	}
	r.Rbsp = true
	return r
}

func (r *NaluReader) ReadByte() (byte, error) {
	if r.Ptr >= len(r.Data) {
		return 0, nil
	}

	if r.Rbsp {
		if len(r.Buf) > 0 {
			b := r.Buf[0]
			r.Buf = r.Buf[1:]
			return b, nil
		}

		if r.Ptr+3 <= len(r.Data) && util.ReadB24(r.Data[r.Ptr:r.Ptr+3]) == 0x000003 {
			r.Buf = r.Data[r.Ptr+1 : r.Ptr+2]
			r.Ptr += 3
			return 0, nil
		}
	}

	b := r.Data[r.Ptr]
	r.Ptr++
	return b, nil
}

type BitReader struct {
	Reader io.ByteReader
	Init   bool
	Byte   byte
	Bit    uint8
}

func (b *BitReader) Read(nbits uint64) uint64 {
	if !b.Init {
		b.Byte, _ = b.Reader.ReadByte()
		b.Bit = 0
		b.Init = true
	}
	res := uint64(0)
	for i := uint64(0); i < nbits; i++ {
		if b.Bit == 8 {
			b.Byte, _ = b.Reader.ReadByte()
			b.Bit = 0
		}
		res <<= 1
		res |= uint64(b.Byte>>(7-b.Bit)) & 1
		b.Bit++
	}
	return res
}

func ReadExpGolomb(b *BitReader) uint64 {
	lzero := uint64(0)
	for b.Read(1) == 0 {
		lzero++
	}
	return 1<<lzero + b.Read(lzero) - 1
}

/*
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
	lzero := uint64(0)
	for ReadBits(data, bitptr, 1) == 0 {
		lzero++
	}
	return 1<<lzero + ReadBits(data, bitptr, lzero) - 1
}
*/
