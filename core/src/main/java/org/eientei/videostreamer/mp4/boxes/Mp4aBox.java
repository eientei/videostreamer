package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4AudioTrack;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4aBox extends Box {
    private final Mp4AudioTrack track;
    private final EsdsBox esds;

    public Mp4aBox(Mp4Context context, Mp4AudioTrack track) {
        super("mp4a", context);
        this.esds = new EsdsBox(context, track);
        this.track = track;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeInt(0);
        out.writeShort(0);

        out.writeShort(track.idx());

        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(track.channels);
        out.writeShort(track.samplesiz);

        out.writeInt(0);
        out.writeShort(track.timescale);
        out.writeShort(track.samplerate);
        esds.write(out);
    }
}
