package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MvexBox extends Mp4Box {
    private List<Mp4TrexBox> trexes = new ArrayList<>();

    public Mp4MvexBox(Mp4RemuxerHandler context, List<Mp4Track> tracks) {
        super("mvex", context);
        for (Mp4Track track : tracks) {
            trexes.add(new Mp4TrexBox(context, track.getId(tracks)));
        }
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (Mp4TrexBox trex : trexes) {
            trex.write(out);
        }
    }
}
