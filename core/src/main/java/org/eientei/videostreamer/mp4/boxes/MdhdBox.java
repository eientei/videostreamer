package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MdhdBox extends FullBox {
    private final Mp4Track track;

    public MdhdBox(Mp4Context context, Mp4Track track) {
        super("mdhd", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        writeVersioned(out, track.timescale, false);
        out.writeShort(0x15C7); // language
        out.writeShort(0); // reserved
    }
}
