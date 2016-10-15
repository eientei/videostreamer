package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpSetPeerBandMessage extends RtmpMessage {
    public RtmpSetPeerBandMessage(int chunkid, long streamid, long time, int size, int type) {
        super(RtmpMessageType.SET_PEER_BAND, chunkid, streamid, time);
        getData().writeInt(size).writeByte(type);
    }

    public RtmpSetPeerBandMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.SET_PEER_BAND, chunkid, streamid, time, data);
    }

    public RtmpSetPeerBandMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    public int getSize() {
        return getData().getInt(0);
    }

    public int getType() {
        return getData().getByte(4);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpSetPeerBandMessage(getHeader(), getData().retain().slice());
    }
}
