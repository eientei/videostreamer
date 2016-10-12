package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-09
 */
public class Url_Box extends FullBox {
    public Url_Box(BoxContext context) {
        super("url ", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeByte(0); // empty string
    }
}
