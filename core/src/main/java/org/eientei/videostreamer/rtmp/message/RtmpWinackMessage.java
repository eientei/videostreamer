package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpWinackMessage extends RtmpMessage {
    public RtmpWinackMessage(int chunkid, long streamid, long time, int size) {
        super(RtmpMessageType.WINACK, chunkid, streamid, time);
        getData().writeInt(size);
    }

    public RtmpWinackMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.WINACK, chunkid, streamid, time, data);
    }

    public RtmpWinackMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    public int getSize() {
        return getData().getInt(0);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpWinackMessage(getHeader(), getData().retain().slice());
    }
}
