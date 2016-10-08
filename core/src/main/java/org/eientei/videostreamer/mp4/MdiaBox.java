package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdiaBox extends Box {
    private final MetaData meta;
    private final Type type;

    public MdiaBox(MetaData meta, Type type) {
        super("mdia");
        this.meta = meta;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new MdhdBox(meta).build());
        out.write(new HdlrBox(type).build());
        out.write(new MinfBox(meta, type).build());
    }
}
