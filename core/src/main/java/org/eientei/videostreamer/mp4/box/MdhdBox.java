package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdhdBox extends FullBox {
        public MdhdBox(BoxContext context) {
        super("mdhd", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        writeVersioned(out);
        out.writeShort(0x15C7); // language
        out.writeShort(0); // reserved
    }
}
