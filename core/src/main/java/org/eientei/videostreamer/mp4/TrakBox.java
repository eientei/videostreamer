package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrakBox extends Box {
    private final Type type;
    private final MetaData meta;

    public TrakBox(Type type, MetaData meta) {
        super("trak");
        this.type = type;
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new TkhdBox(meta, type).build());
        out.write(new MdiaBox(meta, type).build());
    }
}
