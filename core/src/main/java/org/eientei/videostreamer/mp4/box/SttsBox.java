package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class SttsBox extends FullBox {
    public SttsBox(BoxContext context) {
        super("stts", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // entry count

        //out.writeInt(1); // sample count
        //out.writeInt(1); // sample delta
    }
}
