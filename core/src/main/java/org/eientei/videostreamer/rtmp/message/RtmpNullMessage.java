package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.*;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpNullMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpNullMessage> PARSER = new RtmpMessageParser<RtmpNullMessage>() {
        @Override
        public RtmpNullMessage parse(RtmpUnchunkedMessage msg) {
            return new RtmpNullMessage();
        }
    };

    public RtmpNullMessage() {
        super(0, 0, Type.UNKNOWN0);
    }


    @Override
    public void serialize(ByteBuf buf) {
    }

    @Override
    protected RtmpMessage dupInternal() {
        return new RtmpNullMessage();
    }
}
