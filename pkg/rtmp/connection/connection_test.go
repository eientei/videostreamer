package connection

import (
	"context"
	"testing"
	"time"

	"github.com/eientei/videostreamer/internal/rwpipe"
	"github.com/eientei/videostreamer/pkg/rtmp/message"
	"github.com/stretchr/testify/assert"
)

func TestNewConnection(t *testing.T) {
	a, b := rwpipe.New()
	now := time.Now()
	cona := NewConnection(context.Background(), a, now, 0, nil)
	err := cona.Send(context.Background(), &message.Raw{
		Data: []byte{'a', 'b', 'c'},
	})
	assert.NoError(t, err)

	conb := NewConnection(context.Background(), b, now, 0, nil)
	msg, err := conb.Recv(context.Background())
	assert.NoError(t, err)
	assert.EqualValues(t, now, msg.Timestamp)
	assert.EqualValues(t, 0, msg.Delta)
	assert.EqualValues(t, 2, msg.ChunkID)
	assert.EqualValues(t, 0, msg.StreamID)
	assert.EqualValues(t, 0, msg.Type)
	assert.EqualValues(t, 3, msg.Length)
	assert.EqualValues(t, []byte{'a', 'b', 'c'}, msg.Data)

	err = conb.Send(context.Background(), &message.Raw{
		Data: []byte{'e', 'f', 'g'},
	})
	assert.NoError(t, err)

	msg, err = cona.Recv(context.Background())
	assert.NoError(t, err)
	assert.EqualValues(t, now, msg.Timestamp)
	assert.EqualValues(t, 0, msg.Delta)
	assert.EqualValues(t, 2, msg.ChunkID)
	assert.EqualValues(t, 0, msg.StreamID)
	assert.EqualValues(t, 0, msg.Type)
	assert.EqualValues(t, 3, msg.Length)
	assert.EqualValues(t, []byte{'e', 'f', 'g'}, msg.Data)

	err = cona.Close()
	assert.NoError(t, err)

	time.Sleep(time.Millisecond * 200)

	err = cona.Send(context.Background(), &message.Raw{
		Data: []byte{'a', 'b', 'c'},
	})
	assert.Error(t, err)

	err = conb.Send(context.Background(), &message.Raw{
		Data: []byte{'a', 'b', 'c'},
	})
	assert.Error(t, err)

	msg, err = conb.Recv(context.Background())
	assert.Error(t, err)

	msg, err = cona.Recv(context.Background())
	assert.Error(t, err)
}
