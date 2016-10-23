package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TfdtBox extends Mp4BoxFull {
    private final int time;

    public Mp4TfdtBox(Mp4Context context, Mp4Track track, Mp4Subscriber subscriber) {
        super("tfdt", context, 0, 0);
        time = subscriber.getTick(track.id());
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(time);
    }
}
