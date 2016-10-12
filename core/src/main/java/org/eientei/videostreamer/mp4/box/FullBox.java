package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-09
 */
public abstract class FullBox extends Box {
    protected final int version;
    protected final int flags;

    public FullBox(String atom, BoxContext context, int version, int flags) {
        super(atom, context);
        this.version = version;
        this.flags = flags;
    }

    @Override
    protected final void doWrite(ByteBuf out) {
        out.writeByte(version);
        out.writeMedium(flags);
        fullWrite(out);
    }

    protected void writeVersioned(ByteBuf out) {
        long creationTime = context.get(Type.CREATION_TIME);
        long modificationTime = context.get(Type.MODIFICATION_TIME);
        long timescale = context.get(Type.FRAMERATE);
        long duration = context.get(Type.DURATION);

        if (version == 1) {
            out.writeLong(creationTime);
            out.writeLong(modificationTime);
            out.writeInt((int) timescale);
            out.writeLong(duration);
        } else {
            out.writeInt((int) creationTime);
            out.writeInt((int) modificationTime);
            out.writeInt((int) timescale);
            out.writeInt((int) duration);
        }
    }


    protected abstract void fullWrite(ByteBuf out);
}
