package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Avc1Box extends Box {
    private final AvcCBox avcC;

    public Avc1Box(BoxContext context) {
        super("avc1", context);
        avcC = new AvcCBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        StsdBox.writeSampleEntry(out, 1);

        out.writeShort(0);
        out.writeShort(0);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort((int)context.get(Type.WIDTH));
        out.writeShort((int)context.get(Type.HEIGHT));

        out.writeInt(0x00480000);
        out.writeInt(0x00480000);
        out.writeInt(0);
        out.writeShort(1);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(0x0018);
        out.writeShort(-1);

        avcC.write(out);

        // TODO: verify out.write(new AvcCBox(meta).build());
    }
}
