package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrafBox extends Box {
    private final TfhdBox tfhd;
    private final TfdtBox tfdt;
    private final TrunBox trun;

    public TrafBox(BoxContext context) {
        super("traf", context);
        tfhd = new TfhdBox(context);
        tfdt = new TfdtBox(context);
        trun = new TrunBox(context); //getTrackTyped(0x0400 | 0x0800, 0);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tfhd.write(out);
        //tfdt.write(out);
        trun.write(out);
    }
}
