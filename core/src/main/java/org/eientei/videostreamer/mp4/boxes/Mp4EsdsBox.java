package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4AudioTrackAac;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4EsdsBox extends Mp4BoxFull {
    private final List<Mp4Track> tracks;
    private final Mp4AudioTrackAac track;

    public Mp4EsdsBox(Mp4RemuxerHandler context, List<Mp4Track> tracks, Mp4AudioTrackAac track) {
        super("esds", context, 0, 0);
        this.tracks = tracks;
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeByte(0x03);
        out.writeByte(23+track.getAac().readableBytes());
        out.writeShort(track.getId(tracks));
        out.writeByte(0);

        out.writeByte(0x04);
        out.writeByte(15+track.getAac().readableBytes());
        out.writeByte(0x40);
        out.writeByte(0x15);
        out.writeMedium(0);
        out.writeInt(128000);
        out.writeInt(128000);

        out.writeByte(0x05);
        out.writeByte(track.getAac().readableBytes());
        out.writeBytes(track.getAac());

        out.writeByte(0x06);
        out.writeByte(1);
        out.writeByte(0x02);
    }
}
