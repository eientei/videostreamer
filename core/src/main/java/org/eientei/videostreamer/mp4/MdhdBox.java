package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdhdBox extends Box {
    private final MetaData meta;

    public MdhdBox(MetaData meta) {
        super("mdhd");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(meta.getTimeScale()); // timescale
        out.writeInt(0); // duration
        out.writeShort(0x15C7); // language
        out.writeShort(0); // reserved
    }
}
