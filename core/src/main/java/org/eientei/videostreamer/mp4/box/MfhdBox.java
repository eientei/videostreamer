package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

import static org.eientei.videostreamer.mp4.box.Box.Type.SEQUENCE_NUMBER;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MfhdBox extends FullBox {
    public MfhdBox(BoxContext context) {
        super("mfhd", context, 0, 0);
    }
    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt((int)context.get(SEQUENCE_NUMBER));
    }
}
