package server

import (
	"net"
	"errors"
	"io"
	"math/rand"
	"encoding/binary"
	"crypto/hmac"
	"crypto/sha256"
	"bytes"
	"fmt"
)

const (
	SetChunkSizeMessage = 1
	AcknowledgementMessage = 3
	UserControlMessage = 4
	WindowAcknowledgementSizeMessage = 5
	SetPeerBandwidthMessage = 6
	AudioMessage = 8
	VideoMessage = 9
	MetadataMessage = 18
	CommandMessage = 20
)

const (
	StreamBeginUserEvent = 0
)

const (
	NoAction = iota
	CloseConnAction
)

var (
	handshakeError = errors.New("handshake error")
	invalidChunk = errors.New("invalid chunk")
	serverKey = []byte{
		0x47, 0x65, 0x6e, 0x75, 0x69, 0x6e, 0x65, 0x20,
		0x41, 0x64, 0x6f, 0x62, 0x65, 0x20, 0x46, 0x6c,
		0x61, 0x73, 0x68, 0x20, 0x4d, 0x65, 0x64, 0x69,
		0x61, 0x20, 0x53, 0x65, 0x72, 0x76, 0x65, 0x72,
		0x20, 0x30, 0x30, 0x31, // Genuine Adobe Flash Media Server 001
		0xf0, 0xee, 0xc2, 0x4a, 0x80, 0x68, 0xbe, 0xe8,
		0x2e, 0x00, 0xd0, 0xd1, 0x02, 0x9e, 0x7e, 0x57,
		0x6e, 0xec, 0x5d, 0x2d, 0x29, 0x80, 0x6f, 0xab,
		0x93, 0xb8, 0xe6, 0x36, 0xcf, 0xeb, 0x31, 0xae,
	}
	serverKeyPub = serverKey[:36]
	clientKey = []byte{
		0x47, 0x65, 0x6E, 0x75, 0x69, 0x6E, 0x65, 0x20,
		0x41, 0x64, 0x6F, 0x62, 0x65, 0x20, 0x46, 0x6C,
		0x61, 0x73, 0x68, 0x20, 0x50, 0x6C, 0x61, 0x79,
		0x65, 0x72, 0x20, 0x30, 0x30, 0x31, /* Genuine Adobe Flash Player 001 */
		0xF0, 0xEE, 0xC2, 0x4A, 0x80, 0x68, 0xBE, 0xE8,
		0x2E, 0x00, 0xD0, 0xD1, 0x02, 0x9E, 0x7E, 0x57,
		0x6E, 0xEC, 0x5D, 0x2D, 0x29, 0x80, 0x6F, 0xAB,
		0x93, 0xB8, 0xE6, 0x36, 0xCF, 0xEB, 0x31, 0xAE,
	}
	clientKeyPub = clientKey[:30]
)

type RtmpMessage struct {
	Chunk uint16
	Timestamp uint32
	Length uint32
	Type uint8
	Stream uint32
	Delta uint32
	Data []byte
	Full bool
	Action int
}

type RtmpClient struct {
	Conn net.Conn
	InChunk []byte
	OutChunk []byte
	OutMessageQueue chan *RtmpMessage
	Assembly map[uint16]*RtmpMessage
	Remember map[uint16]*RtmpMessage
	Unacked uint32
	Acksize uint32
	Media *Media
	Stream string
}

func RtmpCalcDigestPos(data []byte, base int) int {
	sum := int(data[base]) + int(data[base+1]) + int(data[base+2]) + int(data[base+3])
	return (sum % 728) + base + 4
}

func RtmpDigestPos(data []byte, encrypted bool) int {
	if encrypted {
		return RtmpCalcDigestPos(data, 772)
	} else {
		return RtmpCalcDigestPos(data, 8)
	}
}

func RtmpCalcDigest(data []byte, gappos int, key []byte) []byte {
	mac := hmac.New(sha256.New, key)
	if gappos < 0 {
		mac.Write(data)
	} else {
		mac.Write(data[:gappos])
		mac.Write(data[gappos+sha256.Size:])
	}
	return mac.Sum(nil)
}

func RtmpFindDigest(data []byte, key []byte, encrypted bool) []byte {
	pos := RtmpDigestPos(data, encrypted)
	digest := RtmpCalcDigest(data, pos, key)
	if bytes.Equal(data[pos:pos+len(digest)], digest) {
		return digest
	}

	return nil
}

func RtmpImprintDigest(data []byte, encrypted bool, key []byte) {
	pos := RtmpDigestPos(data, encrypted)
	digest := RtmpCalcDigest(data, pos, key)
	copy(data[pos:pos+len(digest)], digest)
}

func RtmpHandshake(server *Server, conn net.Conn) error {
	cbuf := make([]byte, 1537)
	if _, err := io.ReadFull(conn, cbuf); err != nil {
		return err
	}

	if cbuf[0] != 0x03 {
		return handshakeError
	}

	sbuf := make([]byte, 1537)
	sbuf[0] = 0x03
	rand.Read(sbuf[9:])

	if binary.BigEndian.Uint32(cbuf[5:9]) == 0 {
		conn.Write(sbuf)
		conn.Write(cbuf[1:])
	} else {
		digest := RtmpFindDigest(cbuf[1:], clientKeyPub, false)
		if digest == nil {
			return handshakeError
		}

		interm := RtmpCalcDigest(digest, -1, serverKey)
		response := RtmpCalcDigest(cbuf[1:], 1536 - len(interm), interm)
		copy(cbuf[1537 - len(response):], response)

		RtmpImprintDigest(sbuf[1:], false, serverKeyPub)

		conn.Write(sbuf)
		conn.Write(cbuf[1:])
	}

	if _, err := io.ReadFull(conn, cbuf[1:]); err != nil {
		return err
	}

	return nil
}

func ServeStream(stream *Stream) {
	initbuf := &bytes.Buffer{}
	initbuf.Write(stream.Ftyp)
	initbuf.Write(stream.Moov)

	fmt.Println("Stream", stream.Name, "open")

	for {
		av, ok := <- stream.Data
		if !ok {
			break
		}
		n := 0
		keyframe, _ := FindNalus(av, 5)
		for i, client := range stream.Clients {
			if !client.Init {
				if !keyframe {
					continue
				}
				//fmt.Println("Keyframe")
				client.Init = true
				if _, err := client.Conn.Write(initbuf.Bytes()); err != nil {
					stream.Clients = append(stream.Clients[:i-n], stream.Clients[i+1-n:]...)
					n++
					continue
				}
				data := DistributeAvc(av, 0, 0, 0, false)
				client.Sequence++
				client.Buffer.Write(data)
				/*
				iav := &AVData{av.AudioFrames, av.VideoFrames[idx:], av.FrameRate, av.AudioSampleRate, 0, 0}
				data := DistributeAvc(iav, 0, 0, 1, false)
				buf := &bytes.Buffer{}
				buf.Write(stream.First)
				buf.Write(data)
				if _, err := client.Conn.Write(buf.Bytes()); err != nil {
					stream.Clients = append(stream.Clients[:i-n], stream.Clients[i+1-n:]...)
					n++
					continue
				}
				*/
				client.Sequence = 1
				client.AudioStartTime = av.AudioTime
				client.VideoStartTime = av.VideoTime
				fmt.Println(client.AudioStartTime, client.VideoStartTime)
			} else {
				if keyframe && client.Buffer.Len() > 0 {
					if _, err := client.Conn.Write(client.Buffer.Bytes()); err != nil {
						stream.Clients = append(stream.Clients[:i-n], stream.Clients[i+1-n:]...)
						n++
						continue
					}
					client.Buffer = &bytes.Buffer{}
				}
				//fmt.Println(client.AudioStartTime, client.VideoStartTime, av.AudioTime, av.VideoTime)
				data := DistributeAvc(av, av.AudioTime - client.AudioStartTime, av.VideoTime - client.VideoStartTime, client.Sequence, false)
				client.Buffer.Write(data)
				client.Sequence++
			}
		}
	}
	for _, client := range stream.Clients {
		client.Conn.Close()
	}
	fmt.Println("Stream", stream.Name, "close")
}

func RtmpProcessAmf(server *Server, client *RtmpClient, amf []Amf) {
	if len(amf) == 0 || amf[0].Id() != String {
		fmt.Println("Invalid AMF!", amf)
		return
	}
	switch amf[0].(*AmfString).Value {
	case "connect":
		ackbuf := make([]byte, 4)
		binary.BigEndian.PutUint32(ackbuf, client.Acksize)
		client.OutMessageQueue <- &RtmpMessage{
			Chunk:     0x03,
			Timestamp: 0,
			Length:    4,
			Type:      WindowAcknowledgementSizeMessage,
			Stream:    0x00,
			Delta:     0,
			Data:      ackbuf,
			Full:      true,
		}
		bandbuf := make([]byte, 5)
		binary.BigEndian.PutUint32(bandbuf, client.Acksize)
		bandbuf[4] = 0x01
		client.OutMessageQueue <- &RtmpMessage{
			Chunk:     0x03,
			Timestamp: 0,
			Length:    5,
			Type:      SetPeerBandwidthMessage,
			Stream:    0x00,
			Delta:     0,
			Data:      bandbuf,
			Full:      true,
		}
		streambuf := make([]byte, 6)
		binary.BigEndian.PutUint16(streambuf, StreamBeginUserEvent)
		binary.BigEndian.PutUint32(streambuf[2:], 0)
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: 6,
			Type: UserControlMessage,
			Stream: 0x00,
			Delta: 0,
			Data: streambuf,
			Full: true,
		}
		resbuf := &bytes.Buffer{}
		AmfWriteAll(resbuf, []Amf{
			&AmfString{"_result"},
			amf[1].(*AmfNumber),
			&AmfObject{map[string]Amf{
				"fmsVer": &AmfString{"FMS/3,0,1,123"},
				"capabilities": &AmfNumber{31},
			}},
			&AmfObject{map[string]Amf{
				"level": &AmfString{"status"},
				"code": &AmfString{"NetConnection.Connect.Success"},
				"description": &AmfString{"Connection succeeded."},
				"objectEncoding": &AmfNumber{0},
			}},
		})
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: uint32(resbuf.Len()),
			Type: CommandMessage,
			Stream: 0,
			Delta: 0,
			Data: resbuf.Bytes(),
			Full: true,
		}
	case "createStream":
		resbuf := &bytes.Buffer{}
		AmfWriteAll(resbuf, []Amf{
			&AmfString{"_result"},
			amf[1].(*AmfNumber),
			&AmfNull{},
			&AmfNumber{1},
		})
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: uint32(resbuf.Len()),
			Type: CommandMessage,
			Stream: 0,
			Delta: 0,
			Data: resbuf.Bytes(),
			Full: true,
		}
	case "play":
		resbuf := &bytes.Buffer{}
		AmfWriteAll(resbuf, []Amf{
			&AmfString{"onStatus"},
			amf[1].(*AmfNumber),
			&AmfNull{},
			&AmfObject{map[string]Amf{
				"level": &AmfString{"error"},
				"code": &AmfString{"NetStream.Play.StreamNotFound"},
				"description": &AmfString{"Playing not implemented (yet)."},
				"objectEncoding": &AmfNumber{0},
			}},
		})
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: uint32(resbuf.Len()),
			Type: CommandMessage,
			Stream: 0,
			Delta: 0,
			Data: resbuf.Bytes(),
			Full: true,
			Action: CloseConnAction,
		}
	case "publish":
		stream := amf[3].(*AmfString).Value
		if _, ok := server.Streams[stream]; ok {
			resbuf := &bytes.Buffer{}
			AmfWriteAll(resbuf, []Amf{
				&AmfString{"onStatus"},
				amf[1].(*AmfNumber),
				&AmfNull{},
				&AmfObject{map[string]Amf{
					"level": &AmfString{"error"},
					"code": &AmfString{"NetStream.Publish.AlreadyPublishing"},
					"description": &AmfString{"Stream already publishing."},
					"objectEncoding": &AmfNumber{0},
				}},
			})
			client.OutMessageQueue <- &RtmpMessage{
				Chunk: 0x03,
				Timestamp: 0,
				Length: uint32(resbuf.Len()),
				Type: CommandMessage,
				Stream: 0,
				Delta: 0,
				Data: resbuf.Bytes(),
				Full: true,
				Action: CloseConnAction,
			}
			return
		}
		client.Stream = stream
		streambuf := make([]byte, 6)
		binary.BigEndian.PutUint16(streambuf, StreamBeginUserEvent)
		binary.BigEndian.PutUint32(streambuf[2:], 1)
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: 6,
			Type: UserControlMessage,
			Stream: 0x00,
			Delta: 0,
			Data: streambuf,
			Full: true,
		}
		resbuf := &bytes.Buffer{}
		AmfWriteAll(resbuf, []Amf{
			&AmfString{"onStatus"},
			amf[1].(*AmfNumber),
			&AmfNull{},
			&AmfObject{map[string]Amf{
				"level": &AmfString{"status"},
				"code": &AmfString{"NetStream.Publish.Start"},
				"description": &AmfString{"Start publishing."},
				"objectEncoding": &AmfNumber{0},
			}},
		})
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: uint32(resbuf.Len()),
			Type: CommandMessage,
			Stream: 0,
			Delta: 0,
			Data: resbuf.Bytes(),
			Full: true,
		}
	}
}

func RtmpProcessMessage(server *Server, client *RtmpClient, message *RtmpMessage) {
	switch message.Type {
	case CommandMessage:
		amf := AmfReadAll(bytes.NewReader(message.Data))
		RtmpProcessAmf(server, client, amf)
	case WindowAcknowledgementSizeMessage:
		client.Acksize = binary.BigEndian.Uint32(message.Data)
	case AudioMessage:
		AvcAudio(server, client, message)
	case VideoMessage:
		AvcVideo(server, client, message)
	case MetadataMessage:
		AvcMeta(server, client, message.Data)
	case SetChunkSizeMessage:
		csiz := binary.BigEndian.Uint32(message.Data)
		client.InChunk = make([]byte, csiz)
	}
}


func RtmpConverse(server *Server, client *RtmpClient) error {
	fst := make([]byte, 1)
	if _, err := client.Conn.Read(fst); err != nil {
		return err
	}
	client.Unacked += 1

	format := fst[0] >> 6
	id := uint16(fst[0]) & 0x3f
	if id == 0 {
		cid := make([]byte, 1)
		if _, err := client.Conn.Read(cid); err != nil {
			return err
		}
		client.Unacked += 1
		id = 64 + uint16(cid[0])
	} else if id == 1 {
		cid := make([]byte, 2)
		if _, err := client.Conn.Read(cid); err != nil {
			return err
		}
		client.Unacked += 2
		id = 64 + uint16(cid[0]) * 256 + uint16(cid[1])
	}

	if _, ok := client.Assembly[id]; !ok {
		client.Assembly[id] = &RtmpMessage{
			Chunk: id,
		}
	}
	message := client.Assembly[id]
	switch format {
	case 0:
		buf := make([]byte, 11)
		if _, err := client.Conn.Read(buf); err != nil {
			return err
		}
		client.Unacked += 11
		message.Timestamp = BUint24(buf[0:3])
		message.Length = BUint24(buf[3:6])
		message.Type = buf[6]
		message.Stream = BUint32(buf[7:])
		message.Delta = 0
	case 1:
		buf := make([]byte, 7)
		if _, err := client.Conn.Read(buf); err != nil {
			return err
		}
		client.Unacked += 7
		message.Delta = BUint24(buf[0:3])
		message.Length = BUint24(buf[3:6])
		message.Type = buf[6]
		if len(message.Data) == 0 {
			message.Timestamp += message.Delta
		}
	case 2:
		buf := make([]byte, 3)
		if _, err := client.Conn.Read(buf); err != nil {
			return err
		}
		client.Unacked += 3
		message.Delta = BUint24(buf[0:3])
		if len(message.Data) == 0 {
			message.Timestamp += message.Delta
		}
	case 3:
		if len(message.Data) == 0 {
			message.Timestamp += message.Delta
		}
	}

	l := int(message.Length) - len(message.Data)
	if l < 0 {
		fmt.Println("Invalid chunk!", message.Length, len(message.Data))
		message.Data = message.Data[:0]
		return nil
	}
	if l > len(client.InChunk) {
		l = len(client.InChunk)
	}
	if n, err := io.ReadFull(client.Conn, client.InChunk[:l]); err != nil {
		return err
	} else {
		client.Unacked += uint32(n)
		message.Data = append(message.Data, client.InChunk[:n]...)
	}

	if message.Length == uint32(len(message.Data)) {
		RtmpProcessMessage(server, client, message)
		message.Data = message.Data[:0]
	}

	fmt.Println(message.Length, uint32(len(message.Data)), message.Length == uint32(len(message.Data)), client.Unacked, client.Acksize)
	if client.Acksize > 0 && client.Acksize <= client.Unacked {
		ackbuf := make([]byte, 4)
		binary.BigEndian.PutUint32(ackbuf, client.Unacked)
		client.OutMessageQueue <- &RtmpMessage{
			Chunk: 0x03,
			Timestamp: 0,
			Length: 4,
			Type: AcknowledgementMessage,
			Stream: 0,
			Delta: 0,
			Data: ackbuf,
			Full: true,
		}
		client.Unacked = 0
	}

	return nil
}

func RtmpMessageToChunk(server *Server, client *RtmpClient, done chan struct{}) {
	makeChunkBuf := func(message *RtmpMessage, fst int) []byte {
		if message.Chunk > 319 {
			buf := make([]byte, 3)
			buf[0] = byte(1 | (fst << 6))
			binary.BigEndian.PutUint16(buf[1:], message.Chunk - 64)
			return buf
		} else if message.Chunk > 64 {
			buf := make([]byte, 2)
			buf[0] = byte(1 | (fst << 6))
			buf[1] = byte(message.Chunk - 64)
			return buf
		} else {
			buf := make([]byte, 1)
			buf[0] = byte(message.Chunk) | byte(fst << 6)
			return buf
		}
	}

	for {
		if message, ok := <- client.OutMessageQueue; !ok {
			break
		} else {
			full := message.Full
			if _, ok := client.Remember[message.Chunk]; !ok {
				client.Remember[message.Chunk] = message
				full = true
			}
			fst := 0
			old := client.Remember[message.Chunk]
			if !full {
				if old.Stream == message.Stream {
					fst = 1
					if old.Length == message.Length && old.Type == message.Type {
						fst = 2
						if old.Timestamp + old.Delta == message.Timestamp {
							fst = 3
						}
					}
				}
				client.Remember[message.Chunk] = message
			}

			client.Conn.Write(makeChunkBuf(message, fst))

			switch fst {
			case 0:
				buf := make([]byte, 11)
				PutLUint24(buf[0:3], message.Timestamp)
				PutBUint24(buf[3:6], message.Length)
				buf[6] = message.Type
				PutBUint32(buf[7:], message.Stream)
				old.Delta = 0
				client.Conn.Write(buf)
			case 1:
				buf := make([]byte, 7)
				PutLUint24(buf[0:3], message.Delta)
				PutBUint24(buf[3:6], message.Length)
				buf[6] = message.Type
				client.Conn.Write(buf)
			case 2:
				buf := make([]byte, 3)
				PutLUint24(buf[0:3], message.Delta)
				client.Conn.Write(buf)
			case 3:
			}

			data := message.Data
			csiz := len(client.OutChunk)
			for len(data) > 0 {
				m := len(data)
				if m > csiz {
					m = csiz
				}
				buf := data[:m]
				client.Conn.Write(buf)
				if len(data) > csiz {
					data = data[csiz:]
					client.Conn.Write(makeChunkBuf(message, 3))
				} else {
					break
				}
			}

			switch message.Action {
			case CloseConnAction:
				client.Conn.Close()
			}
		}
	}
	close(done)
}

func RtmpServe(server *Server, conn net.Conn) {
	if err := RtmpHandshake(server, conn); err != nil {
		return
	}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		OutMessageQueue: make(chan *RtmpMessage, 8),
		Assembly: make(map[uint16]*RtmpMessage),
		Remember: make(map[uint16]*RtmpMessage),
		Acksize: 0xFFFFFF,
	}
	messageToChunkDone := make(chan struct{})
	go RtmpMessageToChunk(server, client, messageToChunkDone)
	for {
		if err := RtmpConverse(server, client); err != nil {
			break
		}
	}
	if client.Stream != "" {
		if _, ok := server.Streams[client.Stream]; ok {
			close(server.Streams[client.Stream].Data)
		}
		delete(server.Streams, client.Stream)
	}
	close(client.OutMessageQueue)
	<- messageToChunkDone
}

func RtmpServer(server *Server) {
	watch := make(chan WatchEvent)
	go Watchdog(server.RtmpDone, watch)
	for {
		if conn, err := server.RtmpListener.Accept(); err != nil {
			break
		} else {
			watch <- WatchEvent{conn, ConnOpen}
			go func() {
				RtmpServe(server, conn)
				conn.Close()
				watch <- WatchEvent{conn, ConnClose}
			}()
		}
	}

	watch <- WatchEvent{nil, WatchEnd}
}