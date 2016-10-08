package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TfdtBox extends Box {
    private final MetaData meta;

    public TfdtBox(MetaData meta) {
        super("tfdt");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0);
        out.writeInt(meta.getFirstTime());
    }
}
