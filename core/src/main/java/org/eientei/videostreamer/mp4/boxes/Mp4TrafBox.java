package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Frame;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TrafBox extends Mp4Box {
    private final Mp4TfhdBox tfhd;
    private final Mp4TfdtBox tfdt;
    private final Mp4TrunBox trun;

    public Mp4TrafBox(Mp4Context context, Mp4Frame frame, Mp4Track track, Map<Integer, Integer> ticks) {
        super("traf", context);
        this.tfhd = new Mp4TfhdBox(context, track);
        this.tfdt = new Mp4TfdtBox(context, track, ticks);
        this.trun = new Mp4TrunBox(context, frame, track, ticks);
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
