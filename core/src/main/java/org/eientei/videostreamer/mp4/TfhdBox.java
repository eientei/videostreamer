package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TfhdBox extends Box {
    private final Type type;

    public TfhdBox(Type type) {
        super("tfhd");
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0x00020000); // version and flags
        out.writeInt(type.getValue()+1);
    }
}
