package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4HdlrBox extends Mp4BoxFull {
    private final Mp4Track track;

    public Mp4HdlrBox(Mp4Context context, Mp4Track track) {
        super("hdlr", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // predefined
        out.writeBytes(track.getShortHandler().getBytes()); // handler type
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeBytes(track.getLongHandler().getBytes()); // name
        out.writeByte(0); // null-terminator
    }
}
