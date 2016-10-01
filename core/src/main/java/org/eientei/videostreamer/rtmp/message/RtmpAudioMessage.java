package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpAudioMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpAudioMessage> PARSER = new RtmpMessageParser<RtmpAudioMessage>() {
        @Override
        public RtmpAudioMessage parse(RtmpUnchunkedMessage msg) {
            int length = msg.getHeader().getLength();
            byte[] data = new byte[length];
            msg.getData().readBytes(data);
            return new RtmpAudioMessage(data);
        }
    };
    private final byte[] data;

    public RtmpAudioMessage(byte[] data) {
        super(4, 1, Type.AUDIO);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeBytes(getData());
    }

    @Override
    protected RtmpMessage dupInternal() {
        return new RtmpAudioMessage(data);
    }
}
