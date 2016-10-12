package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-09
 */
public class TrexBox extends FullBox {
    public TrexBox(BoxContext context) {
        super("trex", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        int trackId = context.get(Type.TRACK_ID);
        int defaultSampleDescriptionIndex = context.get(Type.DEFAULT_SAMPLE_DESCRIPTION_INDEX);
        int defaultSampleDuration = context.get(Type.DEFAULT_SAMPLE_DURATION);
        int defaultSampleSize = context.get(Type.DEFAULT_SAMPLE_SIZE);
        int defaultSampleFlags = context.get(Type.DEFAULT_SAMPLE_FLAGS);

        out.writeInt(trackId);
        out.writeInt(defaultSampleDescriptionIndex);
        out.writeInt(defaultSampleDuration);
        out.writeInt(defaultSampleSize);
        out.writeInt(defaultSampleFlags);
    }
}
