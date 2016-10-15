package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4aBox extends Box {
    public Mp4aBox(Mp4Context context) {
        super("mp4a", context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        // TODO
    }
}
