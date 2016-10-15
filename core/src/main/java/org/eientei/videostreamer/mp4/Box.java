package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public abstract class Box {
    protected final String atom;
    protected final Mp4Context context;

    public Box(String atom, Mp4Context context) {
        this.atom = atom;
        this.context = context;
    }

    public final void write(ByteBuf out) {
        int idx = out.writerIndex();
        out.writeInt(0);
        out.writeBytes(atom.getBytes());
        doWrite(out);
        out.setInt(idx, out.writerIndex() - idx);
        complete(out);
    }

    protected abstract void doWrite(ByteBuf out);

    protected void complete(ByteBuf out) {
        // to be overriden if needed
    }

    protected void writeMatrix(ByteBuf out, int a, int b, int c, int d, int tx, int ty) {
        out.writeInt(a << 16);
        out.writeInt(b << 16);
        out.writeInt(0);

        out.writeInt(c << 16);
        out.writeInt(d << 16);
        out.writeInt(0);

        out.writeInt(tx << 16);
        out.writeInt(ty << 16);
        out.writeInt(1 << 30);
    }
}
