package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpContext;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpSetPeerBandMessage extends RtmpMessage {
    public RtmpSetPeerBandMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.SET_PEER_BAND, chunk, stream, time, data);
    }

    public RtmpSetPeerBandMessage(int chunk, int stream, int time, RtmpContext context, int size, byte sizetype) {
        super(RtmpMessageType.SET_PEER_BAND, chunk, stream, time, context.ALLOC.alloc(5));
        getData().writeInt(size).writeByte(sizetype);
    }

    public int getSize() {
        return getData().getInt(0);
    }

    public byte getSizetype() {
        return getData().getByte(4);
    }
}
