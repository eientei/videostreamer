package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MfhdBox extends Box {
    private final int sequence;

    public MfhdBox(int sequence) {
        super("mfhd");
        this.sequence = sequence;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0);
        out.writeInt(sequence);
    }
}
