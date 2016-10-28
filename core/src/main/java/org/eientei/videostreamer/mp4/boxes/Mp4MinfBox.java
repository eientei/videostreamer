package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4MinfBox extends Mp4Box {
    private final Mp4DinfBox dinf;
    private final Mp4Track track;
    private final Mp4StblBox stbl;

    public Mp4MinfBox(Mp4Context context, List<Mp4Track> tracks, Mp4Track track) {
        super("minf", context);
        this.dinf = new Mp4DinfBox(context);
        this.track = track;
        this.stbl = new Mp4StblBox(context, tracks, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        track.getMhd().write(out);
        dinf.write(out);
        stbl.write(out);
    }
}
