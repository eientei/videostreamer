package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrafBox extends Box {
    private final MetaData meta;
    private final List<Sample> samples;
    private final Type type;

    public TrafBox(MetaData meta, List<Sample> samples, Type type) {
        super("traf");
        this.meta = meta;
        this.samples = samples;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new TfhdBox(type).build());
        out.write(new TfdtBox(meta).build());
        out.write(new TrunBox(type, meta, samples).build());
    }
}
