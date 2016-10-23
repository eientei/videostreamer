package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4TrexBox extends Mp4BoxFull {
    private final int id;

    public Mp4TrexBox(Mp4Context context, int id) {
        super("trex", context, 0, 0);
        this.id = id;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(id); // trak id
        out.writeInt(1); // default sample description index
        out.writeInt(0); // default sample duration
        out.writeInt(0); // default sample size
        out.writeInt(0); // default sample flags
    }
}
