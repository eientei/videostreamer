package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpSetChunkSizeMessage extends RtmpMessage {
    public RtmpSetChunkSizeMessage(int chunkid, long streamid, long time, int chunksize) {
        super(RtmpMessageType.SET_CHUNK_SIZE, chunkid, streamid, time);
        getData().writeInt(chunksize);
    }

    public RtmpSetChunkSizeMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.SET_CHUNK_SIZE, chunkid, streamid, time, data);
    }

    public RtmpSetChunkSizeMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    public int getChunkSize() {
        return getData().getInt(0);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpSetChunkSizeMessage(getHeader(), getData().retain().slice());
    }
}
