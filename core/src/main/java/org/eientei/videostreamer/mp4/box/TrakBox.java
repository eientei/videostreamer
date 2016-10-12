package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrakBox extends Box {
    private final TkhdBox tkhd;
    private final MdiaBox mdia;

    public TrakBox(BoxContext context) {
        super("trak", context);
        tkhd = new TkhdBox(context);
        mdia = new MdiaBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tkhd.write(out);
        mdia.write(out);
    }
}
