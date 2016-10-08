package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Mp4aBox extends Box {
    private final MetaData meta;

    public Mp4aBox(MetaData meta) {
        super("mp4a");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // reserved
        out.writeShort(0); // reserved
        out.writeShort(1); // data reference index
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeShort(meta.getAudioChannels()); // channel count
        out.writeShort(meta.getAudioSampleSize() * 8); // sample size
        out.writeInt(0); // reserved
        out.writeShort(meta.getTimeScale());
        out.writeShort(meta.getAudioSampleRate());
        out.write(new EsdsBox(meta).build());
    }
}
