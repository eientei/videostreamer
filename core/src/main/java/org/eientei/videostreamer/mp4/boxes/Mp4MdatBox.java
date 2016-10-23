package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MdatBox extends Mp4Box {
    private final Mp4Frame frame;

    public Mp4MdatBox(Mp4Context context, Mp4Frame frame) {
        super("mdat", context);
        this.frame = frame;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (Mp4Track track : context.getTracks()) {
            for (Mp4Sample sample : frame.getSamples(track.id())) {
                out.writeBytes(sample.getData().slice());
            }
        }
    }
}
