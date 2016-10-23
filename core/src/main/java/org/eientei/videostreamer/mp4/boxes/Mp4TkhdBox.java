package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import static org.eientei.videostreamer.mp4.Mp4BoxUtil.writeMatrix;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4TkhdBox extends Mp4BoxFull {
    private final Mp4Track track;

    public Mp4TkhdBox(Mp4Context context, Mp4Track track) {
        super("tkhd", context, 0, 0x07);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(track.id()); // track id
        out.writeInt(0); // reserved
        out.writeInt(0); // duration

        out.writeInt(0); // reserved
        out.writeInt(0); // reserved

        out.writeShort(0); // layer
        out.writeShort(0); // alternate group
        out.writeShort(track.getVolume()); // volume
        out.writeShort(0); // reserved

        writeMatrix(out, 1, 0, 0, 1, 0, 0); // unity matrix

        out.writeInt(track.getWidth() << 16); // width
        out.writeInt(track.getHeight() << 16); // height
    }
}
