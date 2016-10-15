package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MinfBox extends Box {
    private final Mp4Track track;
    private final DinfBox dinf;
    private final StblBox stbl;

    public MinfBox(Mp4Context context, Mp4Track track) {
        super("minf", context);

        this.dinf = new DinfBox(context);
        this.track = track;
        this.stbl = new StblBox(context, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        track.mhd.write(out);
        dinf.write(out);
        stbl.write(out);
    }
}
