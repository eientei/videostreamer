package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class StblBox extends Box {
    private final StsdBox stsd;
    private final StszBox stsz;
    private final StscBox stsc;
    private final SttsBox stts;
    private final StcoBox stco;

    public StblBox(Mp4Context context, Mp4Track track) {
        super("stbl", context);
        this.stsd = new StsdBox(context, track);
        this.stsz = new StszBox(context);
        this.stsc = new StscBox(context);
        this.stts = new SttsBox(context);
        this.stco = new StcoBox(context);

    }

    @Override
    protected void doWrite(ByteBuf out) {
        stsd.write(out);
        stsz.write(out);
        stsc.write(out);
        stts.write(out);
        stco.write(out);
    }
}
