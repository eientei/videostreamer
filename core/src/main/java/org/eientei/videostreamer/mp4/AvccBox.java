package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class AvccBox extends Box {
    private final MetaData meta;

    public AvccBox(MetaData meta) {
        super("avcC");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(meta.getAvcc());
    }
}
