package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpSetPeerBandMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpSetPeerBandMessage> PARSER = new RtmpMessageParser<RtmpSetPeerBandMessage>() {
        @Override
        public RtmpSetPeerBandMessage parse(RtmpUnchunkedMessage msg) {
            long size = msg.getData().readUnsignedInt();
            int type = msg.getData().readUnsignedByte();
            return new RtmpSetPeerBandMessage(size, type);
        }
    };
    private final long size;
    private final int type;

    public RtmpSetPeerBandMessage(long size, int type) {
        super(2, 0, Type.SET_PEER_BAND);
        this.size = size;
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public int getType() {
        return type;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeInt((int) getSize());
        data.writeByte(getType());
    }
}
