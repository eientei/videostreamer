package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TfdtBox extends Mp4BoxFull {
    private final int time;

    public Mp4TfdtBox(Mp4Context context, Mp4Frame frame, Mp4Track track, Mp4SubscriberContext subscriber) {
        super("tfdt", context, 0, 0);
        //time = (int) ((int) frame.getMinTimestamp(track) - subscriber.getBegin())*1000;
        time = subscriber.getTracktimes().get(track).intValue();
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(time);
    }
}
