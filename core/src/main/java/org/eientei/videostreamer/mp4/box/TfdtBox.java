package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TfdtBox extends FullBox {
    public TfdtBox(BoxContext context) {
        super("tfdt", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        int seq = (int)context.get(Type.SEQUENCE_NUMBER) - 1;
        int buf = context.get(Type.BUFFER);
        int dt = seq * buf;

        if (version == 1) {
            out.writeLong(0);
        } else {
            out.writeInt(0);
        }
    }
}
