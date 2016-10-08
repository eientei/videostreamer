package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrunBox extends Box {
    private final Type type;
    private final MetaData meta;
    private final List<Sample> samples;

    public TrunBox(Type type, MetaData meta, List<Sample> samples) {
        super("trun");
        this.type = type;
        this.meta = meta;
        this.samples = samples;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        int nitems = 0;
        int flags = 0x01;

        if (type == Type.VIDEO || type == Type.AUDIO) {
            nitems++;
            flags |= 0x000100;
            nitems++;
            flags |= 0x000200;
        }

        if (type == Type.VIDEO) {
            nitems++;
            flags |= 0x000400;
            nitems++;
            flags |= 0x000800;
        }

        out.writeInt(flags);
        out.writeInt(samples.size());
        out.writeInt(0);

        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            if (type == Type.VIDEO || type == Type.AUDIO) {
                out.writeInt(sample.getDuration());
                out.writeInt(sample.getSize());
            }
            if (type == Type.VIDEO) {
                out.writeInt(sample.isKey() ? 0x00000000 : 0x00010000);
                out.writeInt(sample.getDelay());
            }
        }
    }
}
