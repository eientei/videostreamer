package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MvhdBox extends FullBox {
    public MvhdBox(BoxContext context) {
        super("mvhd", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        writeVersioned(out);

        out.writeInt(0x00010000); // 1.0
        out.writeShort(0x0100); // full volume
        out.writeShort(0);
        out.writeInt(0);
        out.writeInt(0);
        writeMatrix(out, 1, 0, 0, 1, 0, 0);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeInt((int) context.get(Type.TRACK_ID));
    }
}
