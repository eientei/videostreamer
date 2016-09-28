package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpAckMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpAckMessage> PARSER = new RtmpMessageParser<RtmpAckMessage>() {
        @Override
        public RtmpAckMessage parse(RtmpUnchunkedMessage msg) {
            long size = msg.getData().readUnsignedInt();
            return new RtmpAckMessage(size);
        }
    };

    private final long size;

    public RtmpAckMessage(long size) {
        super(0, 0, Type.ACK);
        this.size = size;
    }

    public long getSize() {
        return size;
    }


    @Override
    public void serialize(ByteBuf data) {
        data.writeInt((int) getSize());
    }
}
