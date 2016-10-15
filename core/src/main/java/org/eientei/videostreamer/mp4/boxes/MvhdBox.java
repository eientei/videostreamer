package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MvhdBox extends FullBox {

    public MvhdBox(Mp4Context context) {
        super("mvhd", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        writeVersioned(out, context.meta.framerate, false);

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

        out.writeInt(context.tracks.size()+1);
    }
}
