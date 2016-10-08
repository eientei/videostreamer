package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TkhdBox extends Box {
    private final MetaData meta;
    private final Type type;

    public TkhdBox(MetaData meta, Type type) {
        super("tkhd");
        this.meta = meta;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeByte(0); // version
        out.writeShort(0); out.writeByte(0x0f); // track enabled, 24-bit
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(type.getValue()+1); // track id
        out.writeInt(0); // reserved
        out.writeInt(0); // duration
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeShort(type == Type.VIDEO ? 0 : 0x0100); // reserved
        out.writeShort(0); // reserved
        writeMatrix(out, 1, 0, 0, 1, 0, 0);
        if (type == Type.VIDEO) {
            out.writeInt(meta.getWidth() << 16);
            out.writeInt(meta.getHeight() << 16);
        } else {
            out.writeInt(0);
            out.writeInt(0);
        }
    }
}
