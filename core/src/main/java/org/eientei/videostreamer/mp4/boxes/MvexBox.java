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
public class MvexBox extends Box {
    private List<TrexBox> trexes = new ArrayList<>();

    public MvexBox(Mp4Context context) {
        super("mvex", context);
        for (Mp4Track track : context.tracks) {
            trexes.add(new TrexBox(context, track.idx()));
        }
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (TrexBox trex : trexes) {
            trex.write(out);
        }
    }
}
