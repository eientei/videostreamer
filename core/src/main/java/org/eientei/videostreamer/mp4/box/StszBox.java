package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class StszBox extends FullBox {
    public StszBox(BoxContext context) {
        super("stsz", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1);
        out.writeInt(0);
    }
}
