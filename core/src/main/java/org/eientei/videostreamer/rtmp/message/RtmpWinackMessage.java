package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpWinackMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpWinackMessage> PARSER = new RtmpMessageParser<RtmpWinackMessage>() {
        @Override
        public RtmpWinackMessage parse(RtmpUnchunkedMessage msg) {
            long size = msg.getData().readUnsignedInt();
            return new RtmpWinackMessage(size);
        }
    };
    private final long size;

    public RtmpWinackMessage(long size) {
        super(2, 0, Type.WINACK);
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
