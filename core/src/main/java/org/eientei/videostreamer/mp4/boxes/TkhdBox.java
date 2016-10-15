package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TkhdBox extends FullBox {
    private final Mp4Track track;

    public TkhdBox(Mp4Context context, Mp4Track track) {
        super("tkhd", context, 0, 0x07);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        writeVersioned(out, track.idx(), true);

        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(0);
        out.writeShort(0);
        out.writeShort(track.volume);
        out.writeShort(0);

        writeMatrix(out, 1, 0, 0, 1, 0, 0);

        out.writeInt(track.width << 16);
        out.writeInt(track.height << 16);
    }
}
