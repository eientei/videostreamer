package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4StsdBox extends Mp4BoxFull {
    private final Mp4Track track;

    public Mp4StsdBox(Mp4Context context, Mp4Track track) {
        super("stsd", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1); // entry count
        track.getInit().write(out);
    }
}
