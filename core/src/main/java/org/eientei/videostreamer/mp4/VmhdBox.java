package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class VmhdBox extends Box {
    public VmhdBox() {
        super("vmhd");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0x01);
        out.writeInt(0);
        out.writeInt(0);
    }
}
