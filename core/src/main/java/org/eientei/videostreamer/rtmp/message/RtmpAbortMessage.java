package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpAbortMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpAbortMessage> PARSER = new RtmpMessageParser<RtmpAbortMessage>() {
        @Override
        public RtmpAbortMessage parse(RtmpUnchunkedMessage msg) {
            long stream = msg.getData().readUnsignedInt();
            return new RtmpAbortMessage(stream);
        }
    };

    private final long stream;

    public RtmpAbortMessage(long stream) {
        super(0, 0, Type.ABORT);
        this.stream = stream;
    }

    public long getStream() {
        return stream;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeInt((int)getStream());
    }
}
