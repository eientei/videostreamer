package server

import (
	"bytes"
	"encoding/binary"
)

const timeBase = 1000
const timeScale = 1

const (
	VideoKeyFrame = 1
	VideoInterFrame = 2
	VideoDisposableFrame = 3
	VideoGeneratedFrame = 4
	VideoInfoCommandFrame = 5
)

const (
	VideoH263Codec = 2
	VideoScreenVideoCodec = 3
	VideoVP6Codec = 4
	VideoVP6AlphaCodec = 5
	VideoScreenVideo2Codec = 6
	VideoAVCCodec = 7
)

const (
	VideoAVCSequence = 0
	VideoAVCNALU = 1
	VideoAVCEOS = 2
)

type Frame struct {
	Basetime uint64
	Data []byte
}

type AVData struct {
	AudioFrames []*Frame
	VideoFrames []*Frame
	FrameRate uint32
	AudioSampleRate uint32
	AudioTime uint64
	VideoTime uint64
}

type Media struct {
	Height uint32
	Width uint32
	FrameRate uint32
	AudioSampleRate uint32
	Sequence uint32
	FirstVCL bool
	AudioFrames []*Frame
	VideoFrames []*Frame
	LastAudioTime uint64
	LastVideoTime uint64
	AudioSkip uint32
	VideoSkip uint32
}

type Sample struct {
	Size uint32
	Duration uint32
}

func AvcAudio(server *Server, client *RtmpClient, message *RtmpMessage) {
	ptr := 0
	format := message.Data[ptr] >> 4
	ptr += 1
	if format != 10 {
		return
	}
	ptr += 1
	datacopy := make([]byte, len(message.Data[ptr:]))
	copy(datacopy, message.Data[ptr:])
	if stream, ok := server.Streams[client.Stream]; ok && stream.First != nil {
		/*
		av := &AVData{
			[]*Frame{{uint64(message.Timestamp), datacopy}},
			nil,
			client.Media.FrameRate,
			client.Media.AudioSampleRate,
			client.Media.LastAudioTime,
			client.Media.LastVideoTime,
		}
		*/
		client.Media.LastAudioTime = uint64(message.Timestamp + (timeBase*timeScale)/(client.Media.AudioSampleRate/1000))
		//server.Streams[client.Stream].Data <- av
		client.Media.AudioFrames = append(client.Media.AudioFrames, &Frame{uint64(message.Timestamp), datacopy})
	}
}

func AvcVideo(server *Server, client *RtmpClient, message *RtmpMessage) {
	ptr := 0
	frame := (message.Data[ptr] >> 4) & 0xF
	codec := message.Data[ptr] & 0xF
	ptr += 1
	avctype := -1
	if codec == VideoAVCCodec {
		avctype = int(message.Data[ptr])
		ptr += 1
		ptr += 3
	}
	if frame == VideoInfoCommandFrame {
		ptr += 1
	} else {
		switch codec {
		case VideoH263Codec:
			return
		case VideoScreenVideoCodec:
			return
		case VideoVP6Codec:
			return
		case VideoVP6AlphaCodec:
			return
		case VideoScreenVideo2Codec:
			return
		case VideoAVCCodec:
			switch avctype {
			case 0:
				DistributeInitAvc(server, client, message.Data[ptr:])
			case 1:
				datacopy := make([]byte, len(message.Data[ptr:]))
				copy(datacopy, message.Data[ptr:])
				if stream, ok := server.Streams[client.Stream]; ok && stream.First != nil {
					av := &AVData{
						client.Media.AudioFrames,
						[]*Frame{{ uint64(message.Timestamp), datacopy}},
						client.Media.FrameRate,
						client.Media.AudioSampleRate,
						client.Media.LastAudioTime,
						client.Media.LastVideoTime,
					}
					client.Media.LastVideoTime = uint64(message.Timestamp + (timeBase * timeScale) / av.FrameRate)
					server.Streams[client.Stream].Data <- av
					client.Media.AudioFrames = nil
					client.Media.VideoFrames = nil
					//}
				} else {
					client.Media.VideoFrames = append(client.Media.VideoFrames, &Frame{ uint64(message.Timestamp), datacopy})
					TryGetVideoInit(server, client)
				}
			}
		}
	}
}

func AvcMeta(server *Server, client *RtmpClient, data []byte) {
	amf := AmfReadAll(bytes.NewReader(data))
	client.Media = &Media{
		Height: uint32(amf[2].(*AmfArray).Value["height"].(*AmfNumber).Value),
		Width: uint32(amf[2].(*AmfArray).Value["width"].(*AmfNumber).Value),
		FrameRate: uint32(amf[2].(*AmfArray).Value["framerate"].(*AmfNumber).Value),
		AudioSampleRate: uint32(amf[2].(*AmfArray).Value["audiosamplerate"].(*AmfNumber).Value),
		Sequence: 0,
	}
}

func DistributeInitAvc(server *Server, client *RtmpClient, avcCdata []byte) {
	ftyp := &FtypBox{
		MajorBand: "isom",
		MinorVersion: 0x200,
		CompatibleBands: []string{"isom", "iso2", "avc1", "mp41"},
	}
	moov := &MoovBox{
		BoxChildren: []Box{
			&MvhdBox{
				CreationTime:0,
				ModificationTime: 0,
				Timescale: timeBase * timeScale,
				Duration: timeBase * timeScale,
				NextTrackId: 0xFFFFFFFF,
			},
			&TrakBox{
				BoxChildren: []Box{
					&TkhdBox{
						CreationTime:0,
						ModificationTime: 0,
						TrackId: 1,
						Duration: timeBase * timeScale,
						Audio: false,
						Width: client.Media.Width<<16,
						Height: client.Media.Height<<16,
					},
					&MdiaBox{
						BoxChildren: []Box{
							&MdhdBox{
								CreationTime: 0,
								ModificationTime: 0,
								Timescale: timeBase * timeScale,
								Duration: timeBase * timeScale,
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
														Width: uint16(client.Media.Width),
														Height: uint16(client.Media.Height),
														Compressorname: "",
														AvcC: &AvcCBox{
															AvcCData: avcCdata,
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
						CreationTime:0,
						ModificationTime: 0,
						TrackId: 2,
						Duration: timeBase * timeScale,
						Audio: true,
						Width: 0,
						Height: 0,
					},
					&MdiaBox{
						BoxChildren: []Box{
							&MdhdBox{
								CreationTime: 0,
								ModificationTime: 0,
								Timescale: timeBase  *timeScale,
								Duration: timeBase * timeScale,
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
														SampleRate: client.Media.AudioSampleRate,
														Esds: &EsdsBox{
															Bitrate: 0,
															Frequency: client.Media.AudioSampleRate,
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
					&MehdBox{
						TimeScale: timeBase * timeScale,
					},
					&TrexBox{
						TrackId: 1,
					},
					&TrexBox{
						TrackId: 2,
					},
				},
			},
		},
	}

	if _, ok := server.Streams[client.Stream]; ok {
		return
	}
	server.Streams[client.Stream] = &Stream{
		Name: client.Stream,
		Data: make(chan *AVData, 4),
		Ftyp: BoxWrite(ftyp),
		Moov: BoxWrite(moov),
	}
}

func TryGetVideoInit(server *Server, client *RtmpClient) {
	av := &AVData{
		client.Media.AudioFrames,
		client.Media.VideoFrames,
		client.Media.FrameRate,
		client.Media.AudioSampleRate,
		0,
		0,
	}
	if ok, _ := FindNalus(av, 6); ok {
		server.Streams[client.Stream].First = DistributeAvc(av, 0, 0, 0, true)
		go ServeStream(server.Streams[client.Stream])
	}
}

func ReadBits(data []byte, bitptr *uint64, nbits uint64) uint64 {
	ptr := *bitptr / 8
	est := (*bitptr + nbits) / 8
	if (*bitptr + nbits) % 8 > 0 {
		est += 1
	}
	if est >= uint64(len(data)) {
		return ^uint64(0)
	}
	off := *bitptr % 8
	sub := data[ptr:est]
	res := uint64(0)
	tod := nbits
	for i, b := range sub {
		lm := tod % 8
		if i == 0 && lm + off > 8 {
			lm -= (lm + off) % 8
		}
		mask := uint64(0)
		for n := uint64(0); n < lm; n++ {
			mask |= 1<<n
		}
		res |= uint64(b>>(8-lm)) & mask
		if i < len(sub)-1 {
			res <<= lm
		}
		tod -= lm
	}
	*bitptr += nbits
	return res
}

func FindNalus(av *AVData, find uint8) (bool, int) {
	for i, video := range av.VideoFrames {
		ptr := uint32(0)
		for {
			l := binary.BigEndian.Uint32(video.Data[ptr:ptr+4]) + 4
			nalutype := video.Data[ptr+4] & 0x1f
			if nalutype == find {
				return true, i
			}
			ptr += l
			if ptr == uint32(len(video.Data)) {
				break
			}
		}
	}
	return false, 0
}

func DistributeAvc(av *AVData, audioTime uint64, videoTime uint64, seq uint32, only56 bool) []byte {
	buf := &bytes.Buffer{}
	videoFrames := uint32(0)
	videos := make([]*Sample, 0)
	for _, video := range av.VideoFrames {
		ptr := uint32(0)
		vs := make([]*Sample, 0)
		for {
			l := binary.BigEndian.Uint32(video.Data[ptr:ptr+4]) + 4
			nalutype := video.Data[ptr+4] & 0x1f
			skip := false
			if nalutype == 12 {
				skip = true
			}
			if only56 && nalutype != 6 {
				skip = true
			}
			if !skip {
				//fmt.Println(nalutype)
				vs = append(vs, &Sample{l, 0})
				buf.Write(video.Data[ptr:ptr+l])
			}
			ptr += l
			if ptr == uint32(len(video.Data)) {
				break
			}
		}
		if len(vs) > 0 {
			videoFrames++
			t := ((timeBase * timeScale) / av.FrameRate) / uint32(len(vs))
			for _, v := range vs {
				v.Duration = t
			}
			videos = append(videos, vs...)
		}
	}
	videoDatalen := buf.Len()

	audioFrames := uint32(0)
	audios := make([]*Sample, 0)
	if !only56 {
		for _, audio := range av.AudioFrames {
			audioFrames++
			audios = append(audios, &Sample{uint32(len(audio.Data)), (timeBase * timeScale) / (av.AudioSampleRate / 1000)})
			buf.Write(audio.Data)
		}
	}

	//fmt.Println(videoFrames, videos, audioFrames, audios)
	moof := &MoofBox{
		BoxChildren: []Box{
			&MfhdBox{
				Sequence: seq,
			},
			&TrafBox{
				BoxChildren: []Box{
					&TfhdBox{
						TrackId: 1,
					},
					&TfdtBox{
						BaseMediaDecodeTime: videoTime,
					},
					&TrunBox{
						SampleSizes: videos,
					},
				},
			},
			&TrafBox{
				BoxChildren: []Box{
					&TfhdBox{
						TrackId: 2,
					},
					&TfdtBox{
						BaseMediaDecodeTime: audioTime,
					},
					&TrunBox{
						SampleSizes: audios,
					},
				},
			},
		},
	}
	mdat := &MdatBox{
		BoxData: buf.Bytes(),
	}

	moofdata := BoxWrite(moof)

	t1off := len(moofdata) - len(audios) * 8 - 4 - 4
	binary.BigEndian.PutUint32(moofdata[t1off:t1off+4], uint32(len(moofdata) + videoDatalen + 8))

	t2off := len(moofdata) -
		len(audios) * 8 - 4 - 4 - 4 - 4 - 4 - 4 - // trun
		8 - 4 - 4 - 4 - // tfdt
		4 - 4 - 4 - 4 - 4 - // tfhd
		4 - 4 - // traf
		len(videos) * 8 - 4 - 4
	binary.BigEndian.PutUint32(moofdata[t2off:t2off+4], uint32(len(moofdata) + 8))

	segment := &bytes.Buffer{}
	segment.Write(moofdata)
	segment.Write(BoxWrite(mdat))

	sumVid := uint32(0)
	for _, s := range videos {
		sumVid += s.Duration
	}
	sumAud := uint32(0)
	for _, s := range audios {
		sumAud += s.Duration
	}
	//fmt.Println(videoTime, sumVid, audioTime, sumAud)

	return segment.Bytes()
}