package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class StsdBox extends FullBox {
    private final Avc1Box avc1;
    private final Mp4aBox mp4a;

    public StsdBox(BoxContext context) {
        super("stsd", context, 0, 0);
        avc1 = new Avc1Box(context);
        mp4a = new Mp4aBox(context);
    }

    public static void writeSampleEntry(ByteBuf out, int dataref) {
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);

        out.writeShort(dataref); // data reference index
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1);
        getTrackTyped(avc1, mp4a).write(out);
    }
}
