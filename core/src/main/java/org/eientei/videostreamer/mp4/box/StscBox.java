package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class StscBox extends FullBox {
    public StscBox(BoxContext context) {
        super("stsc", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // entry count

        //out.writeInt(1); // first_chunk
        //out.writeInt(1); // samples_per_chunk
        //out.writeInt(1); // sample_description_index
    }
}
