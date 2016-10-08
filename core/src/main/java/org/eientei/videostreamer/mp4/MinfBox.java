package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MinfBox extends Box {
    private final MetaData meta;
    private final Type type;

    public MinfBox(MetaData meta, Type type) {
        super("minf");
        this.meta = meta;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        switch (type) {
            case VIDEO:
                out.write(new VmhdBox().build());
                break;
            case AUDIO:
                out.write(new SmhdBox().build());
                break;
        }
        out.write(new DinfBox().build());
        out.write(new StblBox(meta, type).build());
    }
}
