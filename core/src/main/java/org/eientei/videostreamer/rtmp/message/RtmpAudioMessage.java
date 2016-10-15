package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpAudioMessage extends RtmpMessage {
    public RtmpAudioMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.AUDIO, chunkid, streamid, time, data);
    }

    public RtmpAudioMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpAudioMessage(getHeader(), getData().retain().slice());
    }
}
