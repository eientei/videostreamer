package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;
import org.eientei.videostreamer.mp4.Mp4VideoTrack;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Avc1Box extends Box {
    private final AvcCBox avcC;
    private final Mp4Track track;

    public Avc1Box(Mp4Context context, Mp4VideoTrack track) {
        super("avc1", context);
        this.avcC = new AvcCBox(context, track);
        this.track = track;
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

        out.writeShort(track.width);
        out.writeShort(track.height);

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
