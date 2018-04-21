package rtmp

import (
	"bytes"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"errors"
	"io"
	"net"

	"../util"
)

var (
	handshakeError = errors.New("handshake error")
	serverKey      = []byte{
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
	clientKey    = []byte{
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

func CalcDigestPos(data []byte, base int) int {
	sum := int(data[base]) + int(data[base+1]) + int(data[base+2]) + int(data[base+3])
	return (sum % 728) + base + 4
}

func DigestPos(data []byte, encrypted bool) int {
	if encrypted {
		return CalcDigestPos(data, 772)
	} else {
		return CalcDigestPos(data, 8)
	}
}

func CalcDigest(data []byte, gappos int, key []byte) []byte {
	mac := hmac.New(sha256.New, key)
	if gappos < 0 {
		mac.Write(data)
	} else {
		mac.Write(data[:gappos])
		mac.Write(data[gappos+sha256.Size:])
	}
	return mac.Sum(nil)
}

func FindDigest(data []byte, key []byte, encrypted bool) []byte {
	pos := DigestPos(data, encrypted)
	digest := CalcDigest(data, pos, key)
	if bytes.Equal(data[pos:pos+len(digest)], digest) {
		return digest
	}

	return nil
}

func ImprintDigest(data []byte, encrypted bool, key []byte) {
	pos := DigestPos(data, encrypted)
	digest := CalcDigest(data, pos, key)
	copy(data[pos:pos+len(digest)], digest)
}

func Handshake(conn net.Conn) error {
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

	if util.ReadB32(cbuf[5:9]) == 0 {
		if _, err := conn.Write(sbuf); err != nil {
			return err
		}
		if _, err := conn.Write(cbuf[1:]); err != nil {
			return err
		}
	} else {
		digest := FindDigest(cbuf[1:], clientKeyPub, false)
		if digest == nil {
			return handshakeError
		}

		interm := CalcDigest(digest, -1, serverKey)
		response := CalcDigest(cbuf[1:], 1536-len(interm), interm)
		copy(cbuf[1537-len(response):], response)

		ImprintDigest(sbuf[1:], false, serverKeyPub)

		if _, err := conn.Write(sbuf); err != nil {
			return err
		}
		if _, err := conn.Write(cbuf[1:]); err != nil {
			return err
		}
	}

	if _, err := io.ReadFull(conn, cbuf[1:]); err != nil {
		return err
	}

	return nil
}
