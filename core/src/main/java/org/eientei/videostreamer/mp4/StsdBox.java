package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class StsdBox extends Box {
    private final MetaData meta;
    private final Type type;

    public StsdBox(MetaData meta, Type type) {
        super("stsd");
        this.meta = meta;
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version and flags
        out.writeInt(1); // entry count
        switch (type) {
            case VIDEO:
                out.write(new Avc1Box(meta).build());
                break;
            case AUDIO:
                out.write(new Mp4aBox(meta).build());
                break;
        }
    }
}
