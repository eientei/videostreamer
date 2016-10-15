package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpAckMessage extends RtmpMessage {
    public RtmpAckMessage(int chunkid, long streamid, long time, int ackSize) {
        super(RtmpMessageType.ACK, chunkid, streamid, time);
        getData().writeInt(ackSize);
    }

    public RtmpAckMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.ACK, chunkid, streamid, time, data);
    }

    public RtmpAckMessage(RtmpHeader header, ByteBuf data) {
        super(header, data);
    }

    public int getAckSize() {
        return getData().getInt(0);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpAckMessage(getHeader(), getData().retain().slice());
    }
}
