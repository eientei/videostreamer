package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MoofBox extends Box {
    private final MetaData meta;
    private final int sequence;
    private final List<Sample> samples;

    public MoofBox(MetaData meta, int sequence, List<Sample> samples) {
        super("moof");
        this.meta = meta;
        this.sequence = sequence;
        this.samples = samples;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new MfhdBox(sequence).build());
        out.write(new TrafBox(meta, samples, Type.VIDEO).build());
        //out.write(new TrafBox(meta, samples, Type.AUDIO).build());
    }

    @Override
    protected void bake(byte[] data) {
        ByteBuffer wrap = ByteBuffer.wrap(data);
        wrap.position(8); // moof len + 4CC

        wrap.position(wrap.position()+wrap.getInt(wrap.position())); // mfhd skip
        wrap.position(wrap.position()+8); // traf len + 4CC
        wrap.position(wrap.position()+wrap.getInt(wrap.position())); // tfhd skip
        wrap.position(wrap.position()+wrap.getInt(wrap.position())); // tfdt skip
        wrap.position(wrap.position()+8); // trun len + 4CC
        wrap.position(wrap.position()+4+4); // flags + samples
        wrap.putInt(data.length + 8);
    }
}
