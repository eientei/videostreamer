package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4StblBox extends Mp4Box {
    private final Mp4StsdBox stsd;
    private final Mp4StszBox stsz;
    private final Mp4StscBox stsc;
    private final Mp4SttsBox stts;
    private final Mp4StcoBox stco;

    public Mp4StblBox(Mp4Context context, List<Mp4Track> tracks, Mp4Track track) {
        super("stbl", context);
        this.stsd = new Mp4StsdBox(context, tracks, track);
        this.stsz = new Mp4StszBox(context);
        this.stsc = new Mp4StscBox(context);
        this.stts = new Mp4SttsBox(context);
        this.stco = new Mp4StcoBox(context);
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
