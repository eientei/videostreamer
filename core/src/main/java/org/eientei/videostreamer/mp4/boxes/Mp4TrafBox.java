package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TrafBox extends Mp4Box {
    private final Mp4TfhdBox tfhd;
    private final Mp4TfdtBox tfdt;
    private final Mp4TrunBox trun;

    public Mp4TrafBox(Mp4Context context, Mp4Frame frame, List<Mp4Track> tracks, Mp4Track track, Mp4SubscriberContext subscriber) {
        super("traf", context);
        this.tfhd = new Mp4TfhdBox(context, tracks, track);
        this.tfdt = new Mp4TfdtBox(context, frame, track, subscriber);
        this.trun = new Mp4TrunBox(context, frame, track, subscriber);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tfhd.write(out);
        tfdt.write(out);
        trun.write(out);
    }

    public int getOffset() {
        return trun.getOffset();
    }

    public int getCursiz() {
        return trun.getCursiz();
    }
}
