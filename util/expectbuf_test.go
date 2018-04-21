package util

import "testing"

func TestExpectBuf_Write(t *testing.T) {
	if _, err := (&ExpectBuf{Expect: []byte{0x00, 0x01, 0x02}}).Write([]byte{0x00, 0x01, 0x02}); err != nil {
		t.Fatal(err)
	}

	if _, err := (&ExpectBuf{Expect: []byte{0x00, 0x01, 0x02}}).Write([]byte{0x00, 0x03, 0x02}); err == nil {
		t.Fatal()
	}

	if _, err := (&ExpectBuf{Expect: []byte{0x00, 0x01, 0x02}}).Write([]byte{0x00, 0x01, 0x02, 0x03}); err == nil {
		t.Fatal()
	}
}

func TestExpectBuf_Finalize(t *testing.T) {
	buf := &ExpectBuf{Expect: []byte{0x00, 0x01, 0x02}}

	if _, err := buf.Write([]byte{0x00, 0x01, 0x02}); err != nil {
		t.Fatal(err)
	}

	if err := buf.Finalize(); err != nil {
		t.Fatal(err)
	}

	if _, err := buf.Write([]byte{0x00, 0x01}); err != nil {
		t.Fatal(err)
	}

	if _, err := buf.Write([]byte{0x02}); err != nil {
		t.Fatal(err)
	}

	if err := buf.Finalize(); err != nil {
		t.Fatal(err)
	}

	if _, err := buf.Write([]byte{0x00}); err != nil {
		t.Fatal(err)
	}

	if err := buf.Finalize(); err == nil {
		t.Fatal()
	}

	if _, err := buf.Write([]byte{0x00}); err != nil {
		t.Fatal(err)
	}

	if _, err := buf.Write([]byte{0x00}); err == nil {
		t.Fatal()
	}
}
