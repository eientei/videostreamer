package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4VideoTrackH264;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4Avc1Box extends Mp4Box {
    private final Mp4VideoTrackH264 track;
    private final Mp4AvcCBox avcC;

    public Mp4Avc1Box(Mp4Context context, Mp4VideoTrackH264 track) {
        super("avc1", context);
        this.track = track;
        this.avcC = new Mp4AvcCBox(context, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeShort(1);

        out.writeShort(0);
        out.writeShort(0);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(track.getWidth());
        out.writeShort(track.getHeight());

        out.writeInt(0x00480000);
        out.writeInt(0x00480000);
        out.writeInt(0);
        out.writeShort(1);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(0x0018);
        out.writeShort(-1);

        avcC.write(out);
    }
}
