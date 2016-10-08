package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class SmhdBox extends Box {
    public SmhdBox() {
        super("smhd");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0);
        out.writeShort(0);
        out.writeShort(0);
    }
}
