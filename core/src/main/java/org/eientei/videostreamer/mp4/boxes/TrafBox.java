package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TrafBox extends Box {
    private final TfhdBox tfhd;
    private final TfdtBox tfdt;
    private final TrunBox trun;

    public TrafBox(Mp4Context context, Mp4TrackFrame frame) {
        super("traf", context);
        this.tfhd = new TfhdBox(context, frame);
        this.tfdt = new TfdtBox(context, frame);
        this.trun = new TrunBox(context, frame);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tfhd.write(out);
        tfdt.write(out);
        trun.write(out);
    }
}
