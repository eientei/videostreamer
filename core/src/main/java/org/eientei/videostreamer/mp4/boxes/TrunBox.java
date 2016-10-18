package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Sample;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TrunBox extends FullBox {
    private final Mp4TrackFrame frame;

    public TrunBox(Mp4Context context, Mp4TrackFrame frame) {
        super("trun", context, 0, frame.trunFlags);
        this.frame = frame;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(frame.samples.size()); // sample count
        if ((frame.trunFlags & 0x01) != 0) {
            frame.sizptr = out.writerIndex();
            out.writeInt(0); // data offset, post-baked
        }
        if ((frame.trunFlags & 0x04) != 0) {
            out.writeInt(0x2000000);

        }

        for (Mp4Sample sample : frame.samples) {
            if ((frame.trunFlags & 0x100) != 0) {
                out.writeInt(frame.track.frametick); // duration
            }

            if ((frame.trunFlags & 0x200) != 0) {
                out.writeInt(sample.getData().readableBytes());
            }
        }
    }
}
