package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public abstract class FullBox extends Box {
    protected final int version;
    protected final int flags;

    public FullBox(String atom, Mp4Context context, int version, int flags) {
        super(atom, context);
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

    protected void writeVersioned(ByteBuf out, int timescale, boolean addReserve) {
        if (version == 1) {
            out.writeLong(0);
            out.writeLong(0);
            out.writeInt(timescale);
            if (addReserve) {
                out.writeInt(0);
            }
            out.writeLong(0);
        } else {
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(timescale);
            if (addReserve) {
                out.writeInt(0);
            }
            out.writeInt(0);
        }
    }
}
