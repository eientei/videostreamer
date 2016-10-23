package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4VideoTrackH264;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4AvcCBox extends Mp4Box {
    private final Mp4VideoTrackH264 track;

    public Mp4AvcCBox(Mp4Context context, Mp4VideoTrackH264 track) {
        super("avcC", context);
        this.track = track;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeBytes(track.getAvc().slice());
    }
}
