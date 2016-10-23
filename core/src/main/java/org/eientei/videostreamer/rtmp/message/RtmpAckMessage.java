package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpContext;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpAckMessage extends RtmpMessage {
    public RtmpAckMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.ACK, chunk, stream, time, data);
    }

    public RtmpAckMessage(int chunk, int stream, int time, RtmpContext context, int readcount) {
        super(RtmpMessageType.ACK, chunk, stream, time, context.ALLOC.alloc(4));
        getData().writeInt(readcount);
    }

    public int getReadcount() {
        return getData().getInt(0);
    }
}
