package org.eientei.videostreamer.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class PooledAllocator {
    private final PooledByteBufAllocator POOL = new PooledByteBufAllocator();
    private final Set<ByteBuf> buffers = new ConcurrentSet<>();
    private final Logger log = LoggerFactory.getLogger(PooledAllocator.class);

    public ByteBuf allocSizeless() {
        ByteBuf buffer = POOL.buffer();
        buffers.add(buffer);
        return buffer;
    }

    public ByteBuf alloc(int size) {
        ByteBuf buffer = POOL.buffer(size, size);
        buffers.add(buffer);
        return buffer;
    }

    public CompositeByteBuf compose() {
        CompositeByteBuf buffer = POOL.compositeBuffer();
        buffers.add(buffer);
        return buffer;
    }

    public void releasePool() {
        for (ByteBuf buf : buffers) {
            if (buf.refCnt() > 0) {
                log.warn("unfreed buffer {}", buf);
            }
        }
        buffers.clear();
    }

    public String stats() {
        return POOL.dumpStats();
    }
}
