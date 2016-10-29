package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TrunBox extends Mp4BoxFull {
    private final Mp4Frame frame;
    private final Mp4Track track;
    private final Mp4SubscriberContext subscriber;
    private int offset;
    private int cursiz;

    public Mp4TrunBox(Mp4RemuxerHandler context, Mp4Frame frame, Mp4Track track, Mp4SubscriberContext subscriber) {
        super("trun", context, 0, 0x01 | 0x04 | 0x100 | 0x0200);
        this.frame = frame;
        this.track = track;
        this.subscriber = subscriber;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(frame.getSamples(track).size());
        offset = out.writerIndex();
        cursiz = frame.getTotalSize(track);
        out.writeInt(0); // data offset, post-baked
        out.writeInt(0x2000000); // flags
        int diff = (frame.getMaxTimestamp(track) - frame.getMinTimestamp(track) + track.getFrametick());
        int duration = diff / frame.getSamples(track).size();
        for (Mp4Sample sample : frame.getSamples(track)) {
            out.writeInt(duration); // duration
            out.writeInt(sample.getCopydata().readableBytes()); // size
            subscriber.getTracktimes().put(track, subscriber.getTracktimes().get(track) + duration);
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getCursiz() {
        return cursiz;
    }
}
