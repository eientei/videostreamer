package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class StblBox extends Box {
    private final StsdBox stsd;
    private final SttsBox stts;
    private final StscBox stsc;
    private final StszBox stsz;
    private final StcoBox stco;

    public StblBox(BoxContext context) {
        super("stbl", context);
        stsd = new StsdBox(context);
        stts = new SttsBox(context);
        stsc = new StscBox(context);
        stsz = new StszBox(context);
        stco = new StcoBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        stsd.write(out);
        stts.write(out);
        stsc.write(out);
        stsz.write(out);
        stco.write(out);
    }
}
