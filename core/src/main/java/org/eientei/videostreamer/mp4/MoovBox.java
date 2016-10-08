package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MoovBox extends Box {
    private final MetaData meta;

    public MoovBox(MetaData meta) {
        super("moov");
        this.meta = meta;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write(new MvhdBox(meta).build());
        out.write(new MvexBox().build());
        out.write(new TrakBox(Type.VIDEO, meta).build());
        //out.write(new TrakBox(TrakBox.TrakType.AUDIO, meta).build());
    }
}
