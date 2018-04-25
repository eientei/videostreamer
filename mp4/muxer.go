package mp4

import (
	"bytes"
	"fmt"
	"io"
)

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
	Audio = iota
	Video
)

type Nalu struct {
	Reader *NaluReader
	Data   []byte
}

type SampleData struct {
	Sample    *Sample
	Data      []byte
	SliceType uint64
}

type Muxer struct {
	Config       *Config
	Width        uint32
	Height       uint32
	FrameRate    uint32
	AudioRate    uint32
	LengthSize   uint32
	ColorPlanes  bool
	FramenumBits uint32
	MuxHandlers  []MuxHandler
	AudioBuffer  []*SampleData
	VideoBuffer  []*SampleData
}

type MuxEvent struct {
	Atime       uint64
	Vtime       uint64
	PrePresent  []byte
	PreSequence []byte
	PreTrack1   []byte
	PreTrack2   []byte
	Trailer     []byte
}

type MuxHandler interface {
	MuxHandle(mux *MuxEvent)
}

func (muxer *Muxer) Subscribe(handler MuxHandler) {
	muxer.MuxHandlers = append(muxer.MuxHandlers, handler)
}

func NewMuxer(config *Config) *Muxer {
	return &Muxer{
		Config: config,
	}
}

func (muxer *Muxer) Init(width uint32, height uint32, framerate uint32, audiorate uint32, audio []byte, video []byte) []byte {
	muxer.Width = width
	muxer.Height = height
	muxer.FrameRate = framerate
	muxer.AudioRate = audiorate

	ftyp := &FtypBox{
		MajorBand:       "iso5",
		MinorVersion:    1,
		CompatibleBands: []string{"avc1", "iso5", "dash"},
	}
	moov := &MoovBox{
		BoxChildren: []Box{
			&MvhdBox{
				CreationTime:     0,
				ModificationTime: 0,
				Timescale:        1,
				Duration:         0,
				NextTrackId:      0xFFFFFFFF,
			},
			&TrakBox{
				BoxChildren: []Box{
					&TkhdBox{
						CreationTime:     0,
						ModificationTime: 0,
						TrackId:          1,
						Duration:         0,
						Audio:            false,
						Width:            width << 16,
						Height:           height << 16,
					},
					&MdiaBox{
						BoxChildren: []Box{
							&MdhdBox{
								CreationTime:     0,
								ModificationTime: 0,
								Timescale:        1000 * muxer.Config.TimeScale,
								Duration:         0,
							},
							&HdlrBox{
								HandlerType: "vide",
								HandlerName: "Video Handler",
							},
							&MinfBox{
								BoxChildren: []Box{
									&VmhdBox{},
									&DinfBox{
										BoxChildren: []Box{
											&DrefBox{
												BoxChildren: []Box{
													&Url_Box{},
												},
											},
										},
									},
									&StblBox{
										BoxChildren: []Box{
											&StsdBox{
												BoxChildren: []Box{
													&Avc1Box{
														DataReferenceIndex: 1,
														Width:              uint16(width),
														Height:             uint16(height),
														Compressorname:     "",
														AvcC: &AvcCBox{
															AvcCData: video,
														},
													},
												},
											},
											&SttsBox{},
											&StscBox{},
											&StszBox{},
											&StcoBox{},
										},
									},
								},
							},
						},
					},
				},
			},
			&TrakBox{
				BoxChildren: []Box{
					&TkhdBox{
						CreationTime:     0,
						ModificationTime: 0,
						TrackId:          2,
						Duration:         0,
						Audio:            true,
						Width:            0,
						Height:           0,
					},
					&MdiaBox{
						BoxChildren: []Box{
							&MdhdBox{
								CreationTime:     0,
								ModificationTime: 0,
								Timescale:        1000 * muxer.Config.TimeScale,
								Duration:         0,
							},
							&HdlrBox{
								HandlerType: "soun",
								HandlerName: "Sound Handler",
							},
							&MinfBox{
								BoxChildren: []Box{
									&SmhdBox{},
									&DinfBox{
										BoxChildren: []Box{
											&DrefBox{
												BoxChildren: []Box{
													&Url_Box{},
												},
											},
										},
									},
									&StblBox{
										BoxChildren: []Box{
											&StsdBox{
												BoxChildren: []Box{
													&Mp4aBox{
														DataReferenceIndex: 1,
														SampleRate:         audiorate,
														Esds: &EsdsBox{
															Bitrate:   0,
															Frequency: audiorate,
														},
													},
												},
											},
											&SttsBox{},
											&StscBox{},
											&StszBox{},
											&StcoBox{},
										},
									},
								},
							},
						},
					},
				},
			},
			&MvexBox{
				BoxChildren: []Box{
					&TrexBox{
						TrackId: 1,
						Defdur:  1000 * muxer.Config.TimeScale / framerate,
					},
					&TrexBox{
						TrackId: 2,
						Defdur:  (1000 * muxer.Config.TimeScale * 1024) / audiorate,
					},
				},
			},
		},
	}

	if video[1] != 66 {
		return nil
	}

	muxer.LengthSize = uint32(video[4]&3) + 1

	b := &BitReader{Reader: bytes.NewReader(video[8:])}
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
				muxer.ColorPlanes = true
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

	muxer.FramenumBits = uint32(ReadExpGolomb(b) + 4)

	buf := &bytes.Buffer{}
	buf.Write(BoxWrite(ftyp))
	buf.Write(BoxWrite(moov))
	return buf.Bytes()
}

func (muxer *Muxer) AddSampleData(sample *Sample, data []byte, typ uint8, slicetyp uint64) {
	switch typ {
	case Audio:
		muxer.AudioBuffer = append(muxer.AudioBuffer, &SampleData{Sample: sample, Data: data, SliceType: slicetyp})
	case Video:
		muxer.VideoBuffer = append(muxer.VideoBuffer, &SampleData{Sample: sample, Data: data, SliceType: slicetyp})
	}

	if uint32(len(muxer.VideoBuffer)) >= muxer.FrameRate*muxer.Config.BufferSeconds {
		vidx := 0
		aidx := 0
		data := make([]byte, 0)

		vsamples := make([]*Sample, 0)
		asamples := make([]*Sample, 0)

		keyframe := false
		for i, seg := range muxer.VideoBuffer {
			if i == 0 && seg.SliceType == 7 {
				keyframe = true
			}
			if uint32(len(muxer.VideoBuffer)) >= muxer.FrameRate*muxer.Config.BufferSeconds {
				break
			}
			vidx++
			pts := i
			if seg.SliceType == 5 || seg.SliceType == 7 {
				M := 0
				for n := i + 1; n < len(muxer.VideoBuffer); n++ {
					if muxer.VideoBuffer[n].SliceType == 5 || muxer.VideoBuffer[n].SliceType == 7 {
						break
					}
					if muxer.VideoBuffer[n].SliceType == 6 {
						M++
					}
				}
				pts += M + 1
			}

			seg.Sample.Scto = uint32(pts - i)
			vsamples = append(vsamples, seg.Sample)
			data = append(data, seg.Data...)
		}

		vdatalen := len(data)

		for _, seg := range muxer.AudioBuffer {
			asamples = append(asamples, seg.Sample)
			data = append(data, seg.Data...)
			aidx++
		}

		vtime := uint32(len(vsamples)) * 1000 * muxer.Config.TimeScale / muxer.FrameRate
		atime := vtime
		/*
			atotal := uint32(len(asamples)) * 1000 * muxer.Config.TimeScale * 1024 / muxer.AudioRate
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
		*/

		moof := &MoofBox{
			BoxChildren: []Box{
				&MfhdBox{
					Sequence: 0,
				},
				&TrafBox{
					BoxChildren: []Box{
						&TfhdBox{
							TrackId: 1,
						},
						&TfdtBox{
							BaseMediaDecodeTime: 0,
						},
						&TrunBox{
							Samples: vsamples,
						},
					},
				},
				&TrafBox{
					BoxChildren: []Box{
						&TfhdBox{
							TrackId: 2,
						},
						&TfdtBox{
							BaseMediaDecodeTime: 0,
						},
						&TrunBox{
							Samples: asamples,
						},
					},
				},
			},
		}

		moofdata := BoxWrite(moof)

		mdat := &MdatBox{
			BoxData: data,
		}

		mdata := BoxWrite(mdat)

		sidx := &SidxBox{
			ReferenceId:        1,
			Timescale:          1000 * muxer.Config.TimeScale,
			PresentationTime:   0,
			ReferenceSize:      uint32(len(moofdata)) + uint32(len(mdata)),
			SubsegmentDuration: vtime,
			Keyframe:           keyframe,
		}

		t1off := len(moofdata) - len(asamples)*12 - 4
		WriteB32(moofdata[t1off:t1off+4], uint32(len(moofdata)+8+vdatalen))

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

		WriteB32(moofdata[t2off:t2off+4], uint32(len(moofdata)+8))

		sidxdata := BoxWrite(sidx)

		segment := append(append(sidxdata, moofdata...), mdata...)

		event := &MuxEvent{
			Atime:       uint64(atime),
			Vtime:       uint64(vtime),
			PrePresent:  segment[:sidxPresentOffset],
			PreSequence: segment[sidxPresentOffset+8 : len(sidxdata)+mfhdSequenceOffset],
			PreTrack1:   segment[len(sidxdata)+mfhdSequenceOffset+4 : len(sidxdata)+track1TimeOff],
			PreTrack2:   segment[len(sidxdata)+track1TimeOff+8 : len(sidxdata)+track1TimeOff+track1End2TimeOff+len(vsamples)*12],
			Trailer:     segment[len(sidxdata)+track1TimeOff+len(vsamples)*12+track1End2TimeOff+8:],
		}

		for _, h := range muxer.MuxHandlers {
			h.MuxHandle(event)
		}

		fmt.Println(vidx, aidx)
		muxer.VideoBuffer = muxer.VideoBuffer[vidx:]
		muxer.AudioBuffer = muxer.AudioBuffer[aidx:]
	}
}

func (muxer *Muxer) Audio(data []byte) {
	muxer.AddSampleData(&Sample{Duration: 1000 * muxer.Config.TimeScale * 1024 / muxer.AudioRate, Size: uint32(len(data))}, data, Audio, 0)
}

func (muxer *Muxer) Video(data []byte) {
	nalus := muxer.BreakNals(data)

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
		if muxer.ColorPlanes {
			nalu.Reader.Bitr.Read(2)
		}
		nextframe := nalu.Reader.Bitr.Read(uint64(muxer.FramenumBits))

		if len(segd) > 0 {
			if lframenum != nextframe {
				muxer.AddSampleData(&Sample{Duration: 1000 * muxer.Config.TimeScale / muxer.FrameRate, Size: uint32(len(segd))}, segd, Video, lstyp)
				segd = segd[:0]
			}
		}

		segd = append(segd, nalu.Data...)
		lstyp = nextslice
		lframenum = nextframe
	}

	if len(segd) > 0 {
		muxer.AddSampleData(&Sample{Duration: 1000 * muxer.Config.TimeScale / muxer.FrameRate, Size: uint32(len(segd))}, segd, Video, lstyp)
	}
}

func (muxer *Muxer) BreakNals(data []byte) []*Nalu {
	nalus := make([]*Nalu, 0)
	ptr := uint32(0)
	for {
		length := uint32(0)
		switch muxer.LengthSize {
		case 1:
			length = uint32(data[ptr]) + 1
		case 2:
			length = uint32(ReadB16(data[ptr:ptr+2])) + 2
		case 3:
			length = uint32(ReadB24(data[ptr:ptr+3])) + 3
		case 4:
			length = uint32(ReadB32(data[ptr:ptr+4])) + 4
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

		if r.Ptr+3 <= len(r.Data) && ReadB24(r.Data[r.Ptr:r.Ptr+3]) == 0x000003 {
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
