package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MoofBox extends Box {
    public final Mp4TrackFrame frame;
    private final MfhdBox mfhd;
    private final TrafBox traf;

    public MoofBox(Mp4Context context, Mp4TrackFrame frame) {
        super("moof", context);
        this.mfhd = new MfhdBox(context);
        this.frame = frame;
        this.traf = new TrafBox(context, frame);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mfhd.write(out);
        traf.write(out);
    }
}
