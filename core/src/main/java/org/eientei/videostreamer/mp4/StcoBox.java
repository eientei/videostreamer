package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class StcoBox extends Box {
    public StcoBox() {
        super("stco");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version
        out.writeInt(0); // entry count
    }
}
