package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MvhdBox extends Box {
    private final MetaData meta;

    public MvhdBox(MetaData meta) {
        super("mvhd");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(meta.getTimeScale()); // timescale
        out.writeLong(0); // duration

        // reserved
        out.writeInt(0x00010000);
        out.writeShort(0x0100);
        out.writeShort(0);
        out.writeInt(0);

        writeMatrix(out, 1, 0, 0, 1, 0, 0);

        // reserved
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeInt(1); // next track id
    }
}
