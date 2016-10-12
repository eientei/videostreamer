package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class DrefBox extends FullBox {
    private final Url_Box url_;

    public DrefBox(BoxContext context) {
        super("dref", context, 0, 0x1); // self-contained
        url_ = new Url_Box(context);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1); // entry count
        url_.write(out);
    }
}
