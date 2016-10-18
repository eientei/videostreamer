package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MoofBox extends Box {
    private final MfhdBox mfhd;
    public List<Mp4TrackFrame> frames = new ArrayList<>();

    public MoofBox(Mp4Context context) {
        super("moof", context);
        this.mfhd = new MfhdBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mfhd.write(out);
        for (Mp4Track track : context.tracks) {
            Mp4TrackFrame frame = track.getFrame();
            if (frame == null) {
                continue;
            }
            for (Mp4Sample sample : frame.samples) {
                frame.bytes += sample.getData().readableBytes();
            }
            frames.add(frame);
            new TrafBox(context, frame).write(out);
        }
    }
}
