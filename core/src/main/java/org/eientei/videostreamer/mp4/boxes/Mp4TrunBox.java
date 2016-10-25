package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TrunBox extends Mp4BoxFull {
    private final Mp4Frame frame;
    private final Mp4Track track;
    private final Map<Mp4Track, Integer> times;
    private int offset;
    private int cursiz;

    public Mp4TrunBox(Mp4Context context, Mp4Frame frame, Mp4Track track, Map<Mp4Track, Integer> times) {
        super("trun", context, 0, 0x01 | 0x04 | 0x100 | 0x0200);
        this.frame = frame;
        this.track = track;
        this.times = times;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(frame.getSamples(track).size());
        offset = out.writerIndex();
        cursiz = frame.getTotalSize(track);
        out.writeInt(0); // data offset, post-baked
        out.writeInt(0x2000000); // flags
        for (Mp4Sample sample : frame.getSamples(track)) {
            out.writeInt(track.getFrametick()); // duration
            out.writeInt(sample.getData().readableBytes()); // size
            times.put(track, times.get(track) + track.getFrametick());
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getCursiz() {
        return cursiz;
    }
}
