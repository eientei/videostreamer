package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdatBox extends Box {
    private final byte[] data;

    public MdatBox(byte[] data) {
        super("mdat");
        this.data = data;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(data);
    }
}
