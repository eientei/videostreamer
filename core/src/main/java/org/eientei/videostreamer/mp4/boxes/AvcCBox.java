package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4VideoTrack;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class AvcCBox extends Box {
    private final Mp4VideoTrack track;

    public AvcCBox(Mp4Context context, Mp4VideoTrack track) {
        super("avcC", context);
        this.track = track;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeBytes(track.avc);
    }
}
