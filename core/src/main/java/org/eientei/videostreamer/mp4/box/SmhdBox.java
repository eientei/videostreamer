package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class SmhdBox extends FullBox {
    public SmhdBox(BoxContext context) {
        super("smhd", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeShort(0); // balance
        out.writeShort(0); // reserved
    }
}
