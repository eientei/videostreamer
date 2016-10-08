package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Avc1Box extends Box {
    private final MetaData meta;

    public Avc1Box(MetaData meta) {
        super("avc1");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // reserved
        out.writeShort(0); // reserved
        out.writeShort(1); // data reference index
        out.writeShort(0); // codec stream version
        out.writeShort(0); // codec stream revision
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeShort(meta.getWidth()); // width
        out.writeShort(meta.getHeight()); // height
        out.writeInt(0x00480000); // horizontal resolution 72 dpi
        out.writeInt(0x00480000); // vertical resolution 72 dpi
        out.writeInt(0); // data size
        out.writeShort(1); // frame count

        out.writeInt(0); // compressor name
        out.writeInt(0); // compressor name
        out.writeInt(0); // compressor name
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeShort(0x18); // reserved
        out.writeShort(0xffff); // reserved

        out.write(new AvccBox(meta).build());
    }
}
