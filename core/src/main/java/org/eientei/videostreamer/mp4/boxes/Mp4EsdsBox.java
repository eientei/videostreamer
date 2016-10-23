package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4AudioTrakAac;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4EsdsBox extends Mp4BoxFull {
    private final Mp4AudioTrakAac track;

    public Mp4EsdsBox(Mp4Context context, Mp4AudioTrakAac track) {
        super("esds", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeByte(0x03);
        out.writeByte(23+track.getAac().readableBytes());
        out.writeShort(track.id());
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
