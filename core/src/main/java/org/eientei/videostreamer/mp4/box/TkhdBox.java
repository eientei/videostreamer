package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TkhdBox extends FullBox {
    public TkhdBox(BoxContext context) {
        super("tkhd", context, 0, 0x7);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        long creationTime = context.get(Type.CREATION_TIME);
        long modificationTime = context.get(Type.MODIFICATION_TIME);
        long duration = 1; // context.get(Type.DURATION);
        int trackid = context.get(Type.TRACK_ID);

        if (version == 1) {
            out.writeLong(creationTime);
            out.writeLong(modificationTime);
            out.writeInt(trackid);
            out.writeInt(0); // reserved
            out.writeLong(duration);
        } else {
            out.writeInt((int) creationTime);
            out.writeInt((int) modificationTime);
            out.writeInt(trackid);
            out.writeInt(0); // reserved
            out.writeInt((int) duration);
        }

        // reserved
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(0);
        out.writeShort(0);
        out.writeShort(getTrackTyped(0, 0x01000));
        out.writeShort(0);

        writeMatrix(out, 1, 0, 0, 1, 0, 0);

        out.writeInt((int)context.get(Type.WIDTH) << 16);
        out.writeInt((int)context.get(Type.HEIGHT) << 16);
    }
}
