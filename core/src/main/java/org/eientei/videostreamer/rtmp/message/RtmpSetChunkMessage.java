package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpContext;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpSetChunkMessage extends RtmpMessage {
    public RtmpSetChunkMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.SET_CHUNK_SIZE, chunk, stream, time, data);
    }

    public RtmpSetChunkMessage(int chunk, int stream, int time, RtmpContext context, int chunksize) {
        super(RtmpMessageType.SET_CHUNK_SIZE, chunk, stream, time, context.ALLOC.alloc(4));
        getData().writeInt(chunksize);
    }

    public int getChunksize() {
        return getData().getInt(0);
    }
}
