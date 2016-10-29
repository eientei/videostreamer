package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4StscBox extends Mp4BoxFull {
    public Mp4StscBox(Mp4RemuxerHandler context) {
        super("stsc", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // entry count
    }
}
