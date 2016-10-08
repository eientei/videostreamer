package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class DrefBox extends Box {
    public DrefBox() {
        super("dref");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version and flags
        out.writeInt(1); // entry count
        out.writeInt(0xc); // url size
        out.write("url ".getBytes());
        out.writeInt(0x01);
    }
}
