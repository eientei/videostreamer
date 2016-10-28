package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

import static org.eientei.videostreamer.mp4.Mp4BoxUtil.writeMatrix;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MvhdBox extends Mp4BoxFull {
    private final List<Mp4Track> tracks;

    public Mp4MvhdBox(Mp4Context context, List<Mp4Track> tracks) {
        super("mvhd", context, 0, 0);
        this.tracks = tracks;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt((int)tracks.get(0).getTimescale()); // timescale
        out.writeInt(0); // duration

        out.writeInt(0x00010000); // rate
        out.writeShort(0x0100); // volume
        out.writeShort(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        writeMatrix(out, 1, 0, 0, 1, 0, 0); // unity matrix

        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined

        out.writeInt(context.getTracks().size()+1); // next track id
    }
}
