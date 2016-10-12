package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class VmhdBox extends FullBox {
    public VmhdBox(BoxContext context) {
        super("vmhd", context, 0, 1);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeShort(0); // graphic mode

        out.writeShort(0); // red
        out.writeShort(0); // green
        out.writeShort(0); // blue
    }
}
