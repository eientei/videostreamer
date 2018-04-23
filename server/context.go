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

const sidxPresentOffset = 0 +
	8 + // sidx
	4 + // sidx version and flags
	4 + // sidx refid
	4 // sidx timescale

const mfhdSequenceOffset = 0 +
	8 + // moof
	8 + // mfhd
	4 // mfhd version and flags

const track1TimeOff = 0 +
	8 + // moof
	8 + // mfhd
	4 + // mfhd version and flags
	4 + // sequence
	8 + // traf
	8 + // tfhd
	4 + // tfhd version and flags
	4 + // tfhd trackid
	4 + // tfhd flags
	8 + // tfdt
	4 // tfdt version and flags

const track1End2TimeOff = 0 +
	8 + // tfdt base time
	8 + // trun header
	4 + // trun fullbox
	4 + // trun samplelen
	4 + // trun offset
	/* samples length */
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
	Conn        io.WriteCloser
	Initialized bool
	InitFrame   []byte
	Booted      bool
	FirstVCL    bool
	Sequence    uint32

	AudioStartTime uint64
	VideoStartTime uint64
}

type Segment struct {
	Samples   []*mp4.Sample
	Data      []byte
	SliceType uint64
	Starttime uint64
}

type Msg struct {
	Data []byte
	Time uint64
}

type Stream struct {
	AudioIn chan *Msg
	VideoIn chan *Msg

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
	AudioSecs      uint64
	VideoSecs      uint64
	Closing        bool
	First          bool
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
	defer func() {
		stream.Closing = true
	}()
	for {
		select {
		case msg, ok := <-stream.AudioIn:
			if !ok {
				return
			}
			if err := stream.Audio(msg.Data, msg.Time); err != nil {
				return
			}
		case msg, ok := <-stream.VideoIn:
			if !ok {
				return
			}
			if err := stream.Video(msg.Data, msg.Time); err != nil {
				return
			}
		}
	}
}

func (stream *Stream) Audio(data []byte, time uint64) error {
	ptr := 0
	format := data[ptr] >> 4
	ptr += 1
	if format != 10 {
		return UnknownCodec
	}
	if (data[ptr]) == 0 {
		return nil
	}
	ptr += 1
	return stream.SendAudio(data[ptr:], time)
}

func (stream *Stream) Video(data []byte, time uint64) error {
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
				stream.SkipToKeyframe = false
				stream.First = true
				return stream.InitContainer(data[ptr:])
			case 1:
				return stream.SendVideo(data[ptr:], time)
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
								Timescale:        1000 * tvid,
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
								Timescale:        1000 * tvid,
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
						Defdur:  1000 * tvid / stream.FrameRate,
					},
					&mp4.TrexBox{
						TrackId: 2,
						Defdur:  (1000 * tvid * 1024) / stream.AudioRate,
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

func (stream *Stream) SendAudio(data []byte, time uint64) error {
	copydata := make([]byte, len(data))
	copy(copydata, data)
	return stream.AddSegment([]*mp4.Sample{{Duration: 1000 * tvid * 1024 / stream.AudioRate, Size: uint32(len(data))}}, copydata, Audio, 0, time)
}

func (stream *Stream) SendVideo(data []byte, time uint64) error {
	videos := make([]*mp4.Sample, 0)

	nalus := stream.BreakNals(data)

	segd := make([]byte, 0)

	lframenum := uint64(0)

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
				videos = append(videos, &mp4.Sample{Duration: 1000 * tvid / stream.FrameRate, Size: uint32(len(segd))})
				stream.AddSegment(videos, segd, Video, lstyp, time)

				videos = videos[:0]
				segd = segd[:0]
			}
		}

		segd = append(segd, nalu.Data...)
		lstyp = nextslice
		lframenum = nextframe
	}

	if len(segd) > 0 {
		videos = append(videos, &mp4.Sample{Duration: 1000 * tvid / stream.FrameRate, Size: uint32(len(segd))})
		return stream.AddSegment(videos, segd, Video, lstyp, time)
	}
	return nil
}

func (stream *Stream) SendSegment(data []byte, sidxlen int, vsamples int, asamples int, vtime uint32, atime uint32) error {
	toremove := make([]*Client, 0)
	for _, client := range stream.Clients {
		if !client.Initialized {
			client.VideoStartTime = 0
			client.AudioStartTime = 0
			client.Initialized = true
		}

		off := 0
		util.WriteB64(data[sidxPresentOffset:sidxPresentOffset+8], client.VideoStartTime)
		off += sidxlen
		util.WriteB32(data[off+mfhdSequenceOffset:off+mfhdSequenceOffset+4], client.Sequence)
		util.WriteB64(data[off+track1TimeOff:off+track1TimeOff+8], client.VideoStartTime)
		off += track1TimeOff
		off += vsamples * 12
		util.WriteB64(data[off+track1End2TimeOff:off+track1End2TimeOff+8], client.AudioStartTime)

		client.Sequence++

		if _, err := client.Conn.Write(data); err != nil {
			client.Conn.Close()
			toremove = append(toremove, client)
			continue
		}

		client.AudioStartTime += uint64(atime)
		client.VideoStartTime += uint64(vtime)
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

func (stream *Stream) AddSegment(newsamples []*mp4.Sample, sampledata []byte, typ uint8, slicetyp uint64, time uint64) error {
	if len(stream.AudioBuffer) > 0 && len(stream.VideoBuffer) > 0 && slicetyp == 7 {
		keyframe := false
		data := make([]byte, 0)

		vsamples := make([]*mp4.Sample, 0)
		asamples := make([]*mp4.Sample, 0)

		for i, seg := range stream.VideoBuffer {
			pts := i
			if seg.SliceType == 7 {
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
				vsamples = append(vsamples, sample)
			}
			data = append(data, seg.Data...)
		}

		vdatalen := len(data)

		for _, seg := range stream.AudioBuffer {
			asamples = append(asamples, seg.Samples...)
			data = append(data, seg.Data...)
		}

		vtime := uint32(len(vsamples)) * 1000 * tvid / stream.FrameRate
		atime := vtime

		atotal := uint32(0)
		for range asamples {
			atotal += 1000 * tvid * 1024 / stream.AudioRate
		}
		if atotal < vtime {
			amiss := vtime - atotal
			each := amiss / uint32(len(asamples))
			first := amiss % uint32(len(asamples))
			for i, s := range asamples {
				if i == 0 {
					s.Duration += first
				}
				s.Duration += each
			}
		} else if atotal > vtime {
			amiss := atotal - vtime
			each := amiss / uint32(len(asamples))
			first := amiss % uint32(len(asamples))
			for i, s := range asamples {
				if i == 0 {
					s.Duration -= first
				}
				s.Duration -= each
			}
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
							Samples: vsamples,
						},
					},
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
							Samples: asamples,
						},
					},
				},
			},
		}

		moofdata := mp4.BoxWrite(moof)

		mdat := &mp4.MdatBox{
			BoxData: data,
		}

		mdata := mp4.BoxWrite(mdat)

		//atime := uint32(len(asamples)) * 1000 * tvid * 1024 / stream.AudioRate

		sidx := &mp4.SidxBox{
			ReferenceId:        1,
			Timescale:          1000 * tvid,
			PresentationTime:   0,
			ReferenceSize:      uint32(len(moofdata)) + uint32(len(mdata)),
			SubsegmentDuration: vtime,
			Keyframe:           keyframe,
		}

		t1off := len(moofdata) - len(asamples)*12 - 4
		util.WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8+vdatalen))

		t2off := len(moofdata) - len(asamples)*12 - 4 -
			4 - // trun sample no
			4 - // trun fullbox
			8 - // trun header
			8 - // tfdt base time
			4 - // tfdt fullbox
			8 - // tfdt header
			4 - // tfhd flags
			4 - // tfhd trackid
			4 - // tfhd fullbox
			8 - // tfhd header
			8 - // traf header
			len(vsamples)*12 - 4

		util.WriteB32(moofdata[t2off:t2off+4], uint32(len(moofdata)+8))

		sidxdata := mp4.BoxWrite(sidx)

		stream.SendSegment(append(append(sidxdata, moofdata...), mdata...), len(sidxdata), len(vsamples), len(asamples), vtime, atime)

		stream.VideoBuffer = stream.VideoBuffer[:0]
		stream.AudioBuffer = stream.AudioBuffer[:0]
	}

	switch typ {
	case Audio:
		stream.AudioBuffer = append(stream.AudioBuffer, &Segment{Samples: newsamples, Data: sampledata, SliceType: slicetyp, Starttime: time})
	case Video:
		stream.VideoBuffer = append(stream.VideoBuffer, &Segment{Samples: newsamples, Data: sampledata, SliceType: slicetyp, Starttime: time})
	}
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
			length = uint32(util.ReadB32(data[ptr:ptr+4])) + 4
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
