package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class StscBox extends FullBox {
    public StscBox(Mp4Context context) {
        super("stsc", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0);
    }
}
