package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public abstract class Mp4BoxFull extends Mp4Box {
    protected final int version;
    protected final int flags;

    public Mp4BoxFull(String fcc, Mp4Context context, int version, int flags) {
        super(fcc, context);
        this.version = version;
        this.flags = flags;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeByte(version);
        out.writeMedium(flags);
        fullWrite(out);
    }

    protected abstract void fullWrite(ByteBuf out);
}
