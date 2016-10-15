package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public abstract class RtmpMessage {
    private final static PooledByteBufAllocator ALLOC = new PooledByteBufAllocator();

    private final RtmpHeader header;
    private final ByteBuf data;

    public RtmpMessage(RtmpHeader header, ByteBuf data) {
        this.header = header;
        this.data = data;
    }

    public RtmpMessage(RtmpMessageType type, int chunkid, long streamid, long time, ByteBuf data) {
        this(new RtmpHeader(type, chunkid, streamid, time), data);
    }

    public RtmpMessage(RtmpMessageType type, int chunkid, long streamid, long time) {
        this(type, chunkid, streamid, time, ALLOC.buffer());
    }

    public void release() {
        data.release();
    }

    public RtmpHeader getHeader() {
        return header;
    }

    public ByteBuf getData() {
        return data;
    }

    public abstract RtmpMessage copy();
}
