package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TfdtBox extends Mp4BoxFull {
    private final int time;

    public Mp4TfdtBox(Mp4Context context, Mp4Track track, Map<Integer, Integer> ticks) {
        super("tfdt", context, 0, 0);
        time = ticks.get(track.id());
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(time);
    }
}
