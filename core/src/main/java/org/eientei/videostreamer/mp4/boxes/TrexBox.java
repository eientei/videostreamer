package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TrexBox extends FullBox {
    private final Mp4Context context;
    private final int idx;

    public TrexBox(Mp4Context context, int idx) {
        super("trex", context, 0, 0);
        this.context = context;
        this.idx = idx;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(idx);
        out.writeInt(1);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
    }
}
