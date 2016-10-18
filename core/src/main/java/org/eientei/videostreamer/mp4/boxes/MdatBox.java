package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MdatBox extends Box {
    private final MoofBox moof;

    public MdatBox(Mp4Context context, MoofBox moof) {
        super("mdat", context);
        this.moof = moof;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (Mp4TrackFrame frame : moof.frames) {
            for (Mp4Sample sample : frame.samples) {
                out.writeBytes(sample.getData());
            }
            frame.dispose();
        }
    }
}
