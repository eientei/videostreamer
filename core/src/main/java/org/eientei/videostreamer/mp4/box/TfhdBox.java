package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TfhdBox extends FullBox {
    public TfhdBox(BoxContext context) {
        super("tfhd", context, 0, 0x00020000); // consider 0x00020000 hack
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt((int)context.get(Type.TRACK_ID));
    }
}
