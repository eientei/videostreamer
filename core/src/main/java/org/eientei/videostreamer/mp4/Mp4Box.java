package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**3
 * Created by Alexander Tumin on 2016-10-22
 */
public abstract class Mp4Box {
    protected final String fcc;
    protected final Mp4RemuxerHandler context;
    protected int size;

    public Mp4Box(String fcc, Mp4RemuxerHandler context) {
        this.fcc = fcc;
        this.context = context;
    }

    protected abstract void doWrite(ByteBuf out);

    public final void write(ByteBuf out) {
        int idx = out.writerIndex();
        out.writeInt(0);
        out.writeBytes(fcc.getBytes());
        doWrite(out);
        size = out.writerIndex() - idx;
        out.setInt(idx, size);
        postprocess(out);
    }

    protected void postprocess(ByteBuf out) {
        // to be overriden
    }
}
