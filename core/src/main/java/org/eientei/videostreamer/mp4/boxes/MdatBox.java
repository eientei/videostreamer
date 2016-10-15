package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4TrackFrame;
import org.eientei.videostreamer.mp4.Mp4VideoSample;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MdatBox extends Box {
    private final Mp4TrackFrame frame;

    public MdatBox(Mp4Context context, Mp4TrackFrame frame) {
        super("mdat", context);
        this.frame = frame;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (Mp4VideoSample sample : frame.samples) {
            out.writeBytes(sample.getNaldata());
        }
    }
}
