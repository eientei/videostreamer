package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpVideoMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpVideoMessage> PARSER = new RtmpMessageParser<RtmpVideoMessage>() {
        @Override
        public RtmpVideoMessage parse(RtmpUnchunkedMessage msg) {
            int length = msg.getHeader().getLength();
            byte[] data = new byte[length];
            msg.getData().readBytes(data);
            return new RtmpVideoMessage(data);
        }
    };
    private final byte[] data;

    public RtmpVideoMessage(byte[] data) {
        super(0, 0, Type.VIDEO);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeBytes(getData());
    }
}
