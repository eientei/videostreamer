package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TfhdBox extends FullBox {
    private final Mp4TrackFrame frame;

    public TfhdBox(Mp4Context context, Mp4TrackFrame frame) {
        super("tfhd", context, 0, frame.tfhdFlags);
        this.frame = frame;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(frame.track.idx());
        if ((flags & 0x20) != 0) {
            out.writeInt(frame.tfhdSampleFlags);
        }
    }
}
