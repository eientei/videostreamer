package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MvexBox extends Box {
    public MvexBox() {
        super("mvex");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0x20);
        out.write("trex".getBytes());
        out.writeInt(0); // version and flags
        out.writeInt(1); // track id
        out.writeInt(1); // default sample description index
        out.writeInt(0); // default sample duration
        out.writeInt(0); // default sample size
        out.writeInt(0); // defailt sample flag and key on
    }
}
