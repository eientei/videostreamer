package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TfdtBox extends FullBox {
    private final Mp4TrackFrame frame;

    public TfdtBox(Mp4Context context, Mp4TrackFrame frame) {
        super("tfdt", context, 0, 0);
        this.frame = frame;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        if (version == 1) {
            out.writeLong(frame.basetime);
        } else {
            out.writeInt(frame.basetime);
        }
    }
}
