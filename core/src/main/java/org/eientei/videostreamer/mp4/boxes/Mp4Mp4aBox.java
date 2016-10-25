package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4AudioTrakAac;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4Mp4aBox extends Mp4Box {
    private final Mp4AudioTrakAac track;
    private final Mp4EsdsBox esds;

    public Mp4Mp4aBox(Mp4Context context, Mp4AudioTrakAac track) {
        super("mp4a", context);
        this.esds = new Mp4EsdsBox(context, track);
        this.track = track;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeInt(0);
        out.writeShort(0);

        out.writeShort(1);

        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(track.getChannels());
        out.writeShort(track.getSampleSize());

        out.writeInt(0);
        out.writeShort(track.getTimescale());
        out.writeShort(track.getSampleRate());
        esds.write(out);
    }
}