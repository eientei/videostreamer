package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdiaBox extends Box {
    private final MdhdBox mdhd;
    private final HdlrBox hdlr;
    private final MinfBox minf;

    public MdiaBox(BoxContext context) {
        super("mdia", context);
        mdhd = new MdhdBox(context);
        hdlr = new HdlrBox(context);
        minf = new MinfBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mdhd.write(out);
        hdlr.write(out);
        minf.write(out);
    }
}
