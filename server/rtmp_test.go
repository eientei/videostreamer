package server

import (
	"testing"
	"net"
	"io"
	"bytes"
)

type MockData struct {
	Data []byte
	Error error
}

type MockConn struct {
	net.Conn
	QueueIn chan MockData
}

func (conn *MockConn) Read(b []byte) (int, error) {
	select {
		case q := <-conn.QueueIn:
			copy(b, q.Data)
			return len(q.Data), q.Error
	default:
		return 0, io.EOF
	}
}

func (conn *MockConn) Write(b []byte) (int, error) {
	return len(b), nil
}

func (conn *MockConn) Close() error {
	return nil
}

func TestRtmpHandshakeSimple(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x03
	RtmpImprintDigest(cbuf[1:], false, clientKeyPub)
	conn.QueueIn <- MockData{cbuf, nil}
	conn.QueueIn <- MockData{cbuf[1:], nil}

	if err := RtmpHandshake(&Server{}, conn); err != nil {
		t.Fatal(err)
	}
	close(conn.QueueIn)
}

func TestRtmpHandshakeFull(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x03
	cbuf[5] = 0x01
	RtmpImprintDigest(cbuf[1:], false, clientKeyPub)
	conn.QueueIn <- MockData{cbuf, nil}
	conn.QueueIn <- MockData{cbuf[1:], nil}

	if err := RtmpHandshake(&Server{}, conn); err != nil {
		t.Fatal(err)
	}
	close(conn.QueueIn)
}

func TestRtmpHandshakeFullNoDigest(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x03
	cbuf[5] = 0x01
	conn.QueueIn <- MockData{cbuf, nil}

	if err := RtmpHandshake(&Server{}, conn); err == nil {
		t.Fatal()
	}
	close(conn.QueueIn)
}

func TestRtmpHandshakeInvalidC0(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x00
	conn.QueueIn <- MockData{cbuf, nil}
	if err := RtmpHandshake(&Server{}, conn); err == nil {
		t.Fatal()
	}
	close(conn.QueueIn)
}

func TestRtmpHandshakeUndderC1(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1)
	cbuf[0] = 0x03
	conn.QueueIn <- MockData{cbuf, nil}
	if err := RtmpHandshake(&Server{}, conn); err == nil {
		t.Fatal()
	}
	close(conn.QueueIn)
}

func TestRtmpHandshakeUndderC2(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x03
	conn.QueueIn <- MockData{cbuf, nil}
	conn.QueueIn <- MockData{cbuf[100:], nil}
	if err := RtmpHandshake(&Server{}, conn); err == nil {
		t.Fatal()
	}
	close(conn.QueueIn)
}

func TestRtmpDigestPos(t *testing.T) {
	cbuf := make([]byte, 1536)
	pos := RtmpDigestPos(cbuf, true)
	if pos != 776 {
		t.Fatal()
	}
}

func TestRtmpConverse(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x00, 0x00,
		0x00,
		0x00, 0x00, 0x00, 0x00,
	}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseOne(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x00}, nil}
	conn.QueueIn <- MockData{[]byte{0x00}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseOneErr(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x00}, nil}
	conn.QueueIn <- MockData{[]byte{0x00}, io.EOF}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseTwo(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x01}, nil}
	conn.QueueIn <- MockData{[]byte{0x00, 0x00}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseTwoErr(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x01}, nil}
	conn.QueueIn <- MockData{[]byte{0x00, 0x00}, io.EOF}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseFmtOne(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03 | 0x01 << 6}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x00, 0x00,
		0x00,
	}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseFmtOneErr(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03 | 0x01 << 6}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x00, 0x00,
		0x00,
	}, io.EOF}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseFmtTwo(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03 | 0x02 << 6}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
	}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseFmtTwoErr(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03 | 0x02 << 6}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
	}, io.EOF}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseFmtThree(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03 | 0x03 << 6}, nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseTwoChunk(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x01, 0x00,
		0x00,
		0x00, 0x00, 0x00, 0x00,
	}, nil}
	conn.QueueIn <- MockData{make([]byte, 128), nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseTwoChunkErr(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x01, 0x00,
		0x00,
		0x00, 0x00, 0x00, 0x00,
	}, nil}
	conn.QueueIn <- MockData{make([]byte, 127), io.EOF}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
	}
	RtmpConverse(&Server{}, client)
	close(conn.QueueIn)
}

func TestRtmpConverseAck(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	conn.QueueIn <- MockData{[]byte{0x03}, nil}
	conn.QueueIn <- MockData{[]byte{
		0x00, 0x00, 0x00,
		0x00, 0x01, 0x00,
		0x00,
		0x00, 0x00, 0x00, 0x00,
	}, nil}
	conn.QueueIn <- MockData{make([]byte, 128), nil}
	client := &RtmpClient{
		Conn: conn,
		InChunk: make([]byte, 128),
		OutChunk: make([]byte, 128),
		Assembly: make(map[uint16]*RtmpMessage),
		OutMessageQueue: make(chan *RtmpMessage, 1),
		Acksize: 128,
	}
	RtmpConverse(&Server{}, client)
	close(client.OutMessageQueue)
	close(conn.QueueIn)
}

func TestRtmpMessageToChunk(t *testing.T) {
	conn := &MockConn{nil, nil}
	client := &RtmpClient{
		Conn: conn,
		OutChunk: make([]byte, 128),
		OutMessageQueue: make(chan *RtmpMessage, 1),
		Remember: make(map[uint16]*RtmpMessage),
	}
	done := make(chan struct{})
	go RtmpMessageToChunk(&Server{}, client, done)
	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 0,
		Length: 0,
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 65,
		Timestamp: 0,
		Length: 0,
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 320,
		Timestamp: 0,
		Length: 0,
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 1,
		Length: 0,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 256),
	}

	client.OutMessageQueue <- &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
		Action: CloseConnAction,
	}

	close(client.OutMessageQueue)
	<- done
}

func TestRtmpProcessMessage(t *testing.T) {
	client := &RtmpClient{
		OutMessageQueue: make(chan *RtmpMessage, 1),
	}
	buf := &bytes.Buffer{}
	AmfWrite(buf, &AmfString{""})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: uint32(buf.Len()),
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: buf.Bytes(),
	})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 4,
		Type: WindowAcknowledgementSizeMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 4),
	})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: AudioMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: VideoMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: 0,
		Type: MetadataMessage,
		Stream: 0,
		Delta: 0,
		Data: make([]byte, 0),
	})
	close(client.OutMessageQueue)
}

func TestRtmpProcessAmf(t *testing.T) {
	client := &RtmpClient{
		OutMessageQueue: make(chan *RtmpMessage, 20),
	}
	buf := &bytes.Buffer{}
	AmfWriteAll(buf, []Amf{&AmfString{"connect"}, &AmfNumber{1}})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: uint32(buf.Len()),
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: buf.Bytes(),
	})
	buf.Reset()
	AmfWriteAll(buf, []Amf{&AmfString{"createStream"}, &AmfNumber{2}})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: uint32(buf.Len()),
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: buf.Bytes(),
	})
	buf.Reset()
	AmfWriteAll(buf, []Amf{&AmfString{"play"}, &AmfNumber{2}})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: uint32(buf.Len()),
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: buf.Bytes(),
	})
	buf.Reset()
	AmfWriteAll(buf, []Amf{&AmfString{"publish"}, &AmfNumber{2}})
	RtmpProcessMessage(&Server{}, client, &RtmpMessage{
		Chunk: 3,
		Timestamp: 2,
		Length: uint32(buf.Len()),
		Type: CommandMessage,
		Stream: 0,
		Delta: 0,
		Data: buf.Bytes(),
	})
	close(client.OutMessageQueue)
}

func TestRtmpServe(t *testing.T) {
	conn := &MockConn{nil, make(chan MockData, 4)}
	cbuf := make([]byte, 1537)
	cbuf[0] = 0x03
	RtmpImprintDigest(cbuf[1:], false, clientKeyPub)
	conn.QueueIn <- MockData{cbuf, nil}
	conn.QueueIn <- MockData{cbuf[1:], nil}

	RtmpServe(&Server{}, conn)
	close(conn.QueueIn)
}