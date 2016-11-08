package org.eientei.videostreamer.impl.util;

import com.github.jinahya.bit.io.ByteInput;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class ByteBufInput implements ByteInput {
    private final ByteBuf buf;

    public ByteBufInput(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        if (!buf.isReadable()) {
            return -1;
        }
        return buf.readByte();
    }
}