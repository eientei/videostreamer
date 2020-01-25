package handshake

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"errors"
	"io"
	"math/rand"
	"time"

	"github.com/eientei/videostreamer/internal/byteorder"
)

// Signed handshake halves and offsets
const (
	Stage0Len       = 1
	Stage12Len      = 1536
	StageHeaderLen  = 8
	StageHashLen    = 4
	HalfStageLen    = (Stage12Len - StageHeaderLen) / 2
	HalfStageOffset = HalfStageLen + StageHeaderLen
	AlgorithmFirst  = 0
	AlgorithmLast   = 1
)

// This is key exchange handshake variant

var (
	// FMSServerPrivateKey is private server key
	FMSServerPrivateKey = []byte{
		0x47, 0x65, 0x6e, 0x75, 0x69, 0x6e, 0x65, 0x20,
		0x41, 0x64, 0x6f, 0x62, 0x65, 0x20, 0x46, 0x6c,
		0x61, 0x73, 0x68, 0x20, 0x4d, 0x65, 0x64, 0x69,
		0x61, 0x20, 0x53, 0x65, 0x72, 0x76, 0x65, 0x72,
		0x20, 0x30, 0x30, 0x31,
		0xf0, 0xee, 0xc2, 0x4a, 0x80, 0x68, 0xbe, 0xe8,
		0x2e, 0x00, 0xd0, 0xd1, 0x02, 0x9e, 0x7e, 0x57,
		0x6e, 0xec, 0x5d, 0x2d, 0x29, 0x80, 0x6f, 0xab,
		0x93, 0xb8, 0xe6, 0x36, 0xcf, 0xeb, 0x31, 0xae,
	}

	// FMSServerPublicKey is a public server key
	FMSServerPublicKey = FMSServerPrivateKey[:36]

	// FPClientPrivateKey is a private client key
	FPClientPrivateKey = []byte{
		0x47, 0x65, 0x6e, 0x75, 0x69, 0x6e, 0x65, 0x20,
		0x41, 0x64, 0x6f, 0x62, 0x65, 0x20, 0x46, 0x6c,
		0x61, 0x73, 0x68, 0x20, 0x50, 0x6c, 0x61, 0x79,
		0x65, 0x72, 0x20, 0x30, 0x30, 0x31,
		0xf0, 0xee, 0xc2, 0x4a, 0x80, 0x68, 0xbe, 0xe8,
		0x2e, 0x00, 0xd0, 0xd1, 0x02, 0x9e, 0x7e, 0x57,
		0x6e, 0xec, 0x5d, 0x2d, 0x29, 0x80, 0x6f, 0xab,
		0x93, 0xb8, 0xe6, 0x36, 0xcf, 0xeb, 0x31, 0xae,
	}

	// FPClientPublicKey is a public client key
	FPClientPublicKey = FPClientPrivateKey[:30]
)

var (
	// ErrInvalidDigestOffset indicates digest not located at expected offset
	ErrInvalidDigestOffset = errors.New("invalid digest offset")

	// ErrInvalidDigest indicates invalid digest in handshake
	ErrInvalidDigest = errors.New("invalid digest")
)

// KeysConfig contains options for keyed handshake
type KeysConfig struct {
	PrivateKey    []byte // local party private key
	PublicKey     []byte // local party public key
	PeerPublicKey []byte // remote part public key
	Algorithm     int    // signing algorithm, can be either AlgorithmFirst or AlgorithmLast
}

// NewServerKeysHandshake returns server-side keyed handshake
func NewServerKeysHandshake(config *KeysConfig) Handshaker {
	if config == nil {
		config = &KeysConfig{}
	}

	if config.PrivateKey == nil && config.PublicKey == nil && config.PeerPublicKey == nil {
		config.PrivateKey = FMSServerPrivateKey
		config.PublicKey = FMSServerPublicKey
		config.PeerPublicKey = FPClientPublicKey
	}

	return &keys{
		config: *config,
	}
}

// NewClientKeysHandshake returns client-side keyed handshake
func NewClientKeysHandshake(config *KeysConfig) Handshaker {
	if config == nil {
		config = &KeysConfig{}
	}

	if config.PrivateKey == nil && config.PublicKey == nil && config.PeerPublicKey == nil {
		config.PrivateKey = FPClientPrivateKey
		config.PublicKey = FPClientPublicKey
		config.PeerPublicKey = FMSServerPublicKey
	}

	return &keys{
		config: *config,
	}
}

type keys struct {
	config KeysConfig
}

func (impl *keys) Handshake(rw io.ReadWriter) (timestamp time.Time, peerDelta uint32, err error) {
	timestamp = time.Now()
	buf := make([]byte, Stage0Len+Stage12Len)
	l0 := buf[:1]
	l1 := buf[1:]
	l1time := l1[:4]
	l1ver := l1[4:8]
	l1data := l1[8:]
	l0[0] = 0x03
	l1ver[0] = 0x04
	l1ver[1] = 0x00
	l1ver[2] = 0x00
	l1ver[3] = 0x01

	byteorder.BigEndian.PutUint32(l1time, 0)
	rand.Seed(time.Now().UnixNano())

	_, err = rand.Read(l1data)
	if err != nil {
		return
	}

	err = impl.imprintDigest(l1)
	if err != nil {
		return
	}

	_, err = rw.Write(buf)
	if err != nil {
		return
	}

	_, err = io.ReadFull(rw, buf)
	if err != nil {
		return
	}

	r1 := buf[1:]
	r1time := r1[:4]
	r1ver := r1[4:8]

	hver := byteorder.BigEndian.Uint32(r1ver)
	peerDelta = byteorder.BigEndian.Uint32(r1time)

	if hver != 0 {
		err = impl.imprintSignature(r1, impl.config.PrivateKey, impl.config.PeerPublicKey)
		if err != nil {
			return
		}
	}

	r2 := buf[1:]

	_, err = rw.Write(r2)
	if err != nil {
		return
	}

	_, err = io.ReadFull(rw, l1)
	if err != nil {
		return
	}

	return
}

func (impl *keys) verifyDigest(offset int, data, pubkey []byte) bool {
	digest := impl.calculateDigest(offset, data, pubkey)
	return bytes.Equal(digest, data[offset:offset+sha256.Size])
}

func (impl *keys) calculateDigest(offset int, data, pubkey []byte) []byte {
	hash := hmac.New(sha256.New, pubkey)
	_, _ = hash.Write(data[:offset])
	_, _ = hash.Write(data[offset+sha256.Size:])

	return hash.Sum(nil)
}

func (impl *keys) findDHOffset(peerData []byte, sumOffset int) (offset int, err error) {
	sumdata := peerData[sumOffset : sumOffset+StageHashLen]
	sum := 0

	for i := 0; i < StageHashLen; i++ {
		sum += int(sumdata[i])
	}

	offset = (sum % (HalfStageLen - sha256.Size - StageHashLen)) + sumOffset + StageHashLen

	if sha256.Size+offset >= sumOffset+HalfStageLen {
		return 0, ErrInvalidDigestOffset
	}

	return
}

func (impl *keys) imprintDigest(data []byte) (err error) {
	var offset int

	switch impl.config.Algorithm {
	case AlgorithmLast:
		offset, err = impl.findDHOffset(data, HalfStageOffset)
	default:
		offset, err = impl.findDHOffset(data, StageHeaderLen)
	}

	if err != nil {
		return err
	}

	digest := impl.calculateDigest(offset, data, impl.config.PublicKey)
	copy(data[offset:offset+sha256.Size], digest)

	return nil
}

func (impl *keys) imprintSignature(data, key, peerPubKey []byte) error {
	peerOffset, err := impl.findDHOffset(data, StageHeaderLen)
	if err != nil {
		return err
	}

	if !impl.verifyDigest(peerOffset, data, peerPubKey) {
		peerOffset, err = impl.findDHOffset(data, HalfStageOffset)

		if err != nil {
			return err
		}

		if !impl.verifyDigest(peerOffset, data, peerPubKey) {
			return ErrInvalidDigest
		}
	}

	hash := hmac.New(sha256.New, key)
	_, _ = hash.Write(data[peerOffset : peerOffset+sha256.Size])
	resp := hash.Sum(nil)
	hash = hmac.New(sha256.New, resp)
	_, _ = hash.Write(data[:len(data)-sha256.Size])
	signature := hash.Sum(nil)
	copy(data[len(data)-sha256.Size:], signature)

	return nil
}
