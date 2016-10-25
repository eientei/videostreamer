package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MoovBox extends Mp4Box {
    private final Mp4MvhdBox mvhd;
    private final List<Mp4TrakBox> traks = new ArrayList<>();
    private final Mp4MvexBox mvex;

    public Mp4MoovBox(Mp4Context context, List<Mp4Track> tracks) {
        super("moov", context);
        this.mvhd = new Mp4MvhdBox(context, tracks);
        for (Mp4Track track : tracks) {
            traks.add(new Mp4TrakBox(context, track));
        }
        this.mvex = new Mp4MvexBox(context, tracks);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mvhd.write(out);
        for (Mp4TrakBox trak : traks) {
            trak.write(out);
        }
        mvex.write(out);
    }
}
