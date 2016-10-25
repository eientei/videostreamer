package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MdatBox extends Mp4Box {
    private final List<Mp4Track> tracks;
    private final Mp4Frame frame;

    public Mp4MdatBox(Mp4Context context, List<Mp4Track> tracks, Mp4Frame frame) {
        super("mdat", context);
        this.tracks = tracks;
        this.frame = frame;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        for (Mp4Track track : tracks) {
            if (frame.getSamples(track) != null) {
                for (Mp4Sample sample : frame.getSamples(track)) {
                    out.writeBytes(sample.getData().slice());
                }
            }
        }
    }
}
