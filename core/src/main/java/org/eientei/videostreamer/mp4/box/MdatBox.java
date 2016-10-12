package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.Sample;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MdatBox extends Box {
    public MdatBox(BoxContext context) {
        super("mdat", context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        List<Sample> samples = context.get(Type.SAMPLES);
        for (Sample s : samples) {
            out.writeBytes(s.getMdatbuf());
        }
    }
}
