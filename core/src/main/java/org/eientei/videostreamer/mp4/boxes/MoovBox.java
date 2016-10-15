package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MoovBox extends Box {
    private final MvhdBox mvhd;
    private final List<TrakBox> traks = new ArrayList<>();
    private final MvexBox mvex;

    public MoovBox(Mp4Context context) {
        super("moov", context);
        this.mvhd = new MvhdBox(context);
        for (Mp4Track track : context.tracks) {
            traks.add(new TrakBox(context, track));
        }
        this.mvex = new MvexBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mvhd.write(out);
        for (TrakBox trak : traks) {
            trak.write(out);
        }
        mvex.write(out);
    }
}
