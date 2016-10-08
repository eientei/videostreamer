package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class StblBox extends Box {
    private final MetaData meta;
    private final Type type;

    public StblBox(MetaData meta, Type type) {
        super("stbl");
        this.meta = meta;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new StsdBox(meta, type).build());
        out.write(new SttsBox().build());
        out.write(new StscBox().build());
        out.write(new StszBox().build());
        out.write(new StcoBox().build());
    }
}
