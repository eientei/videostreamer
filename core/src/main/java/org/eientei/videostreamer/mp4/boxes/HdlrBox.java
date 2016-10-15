package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class HdlrBox extends FullBox {
    private final Mp4Track track;

    public HdlrBox(Mp4Context context, Mp4Track track) {
        super("hdlr", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0);
        out.writeBytes(track.shorthandler);
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeBytes(track.longhandler);
        out.writeByte(0); // null-terminator
    }
}
