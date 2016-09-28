package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.PlatformDependent;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpUnchunkedMessage {
    private final static PooledByteBufAllocator POOL = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

    private final RtmpHeader header;
    private final ByteBuf data;

    public RtmpUnchunkedMessage(RtmpHeader header) {
        this.header = header;
        this.data = POOL.buffer();
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
}
