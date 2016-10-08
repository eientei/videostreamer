package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class DinfBox extends Box {
    public DinfBox() {
        super("dinf");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new DrefBox().build());
    }
}
