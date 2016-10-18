package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4AudioTrack;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-16
 */
public class EsdsBox extends FullBox {
    private final Mp4AudioTrack track;

    public EsdsBox(Mp4Context context, Mp4AudioTrack track) {
        super("esds", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeByte(0x03);
        out.writeByte(23+2);
        out.writeShort(track.idx());
        out.writeByte(0);

        out.writeByte(0x04);
        out.writeByte(15+2);
        out.writeByte(0x40);
        out.writeByte(0x15);
        out.writeMedium(0);
        out.writeInt(128000);
        out.writeInt(128000);

        out.writeByte(0x05);
        out.writeByte(2);
        out.writeBytes(track.spec);

        out.writeByte(0x06);
        out.writeByte(1);
        out.writeByte(0x02);
    }
}
