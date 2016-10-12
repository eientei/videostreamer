package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

import static org.eientei.videostreamer.mp4.box.Box.Type.*;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Mp4aBox extends Box {
    public Mp4aBox(BoxContext context) {
        super("mp4a", context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        StsdBox.writeSampleEntry(out, 1);
        out.writeInt(0);
        out.writeInt(0);
        out.writeShort((int)context.get(AUDIO_CHANNELS));
        out.writeShort((int)context.get(AUDIO_SAMPLE_SIZE));
        out.writeShort(0);
        out.writeShort(0);
        out.writeShort((int)context.get(AUDIO_SAMPLE_RATE));
    }
}
