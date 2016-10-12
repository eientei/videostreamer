package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class TrunBox extends FullBox {
    private Logger log = LoggerFactory.getLogger(TrunBox.class);
    public TrunBox(BoxContext context) {
        super("trun", context, 0, 0x0001 | 0x0100 | 0x0200);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        List<Sample> samples = context.get(Type.SAMPLES);
        out.writeInt(samples.size()); // sample count
        out.writeInt(0); // data offset, post-baked
        int seq = (int)context.get(Type.SEQUENCE_NUMBER) - 1;
        int buf = context.get(Type.BUFFER);
        int delay = seq * buf;
        int idx = 0;
        for (Sample s : samples) {
            out.writeInt(s.isFirst() ? 1 : 0); // duration
            out.writeInt(s.getMdatbuf().length);

            if ((flags & 0x0400) != 0) {
                //out.writeInt(s.isKey() ? 0x00000000 : 0x00010000);
                //out.writeInt(0);
            }

            if ((flags & 0x0800) != 0) {
                //out.writeInt(delay+idx);
            }
            idx++;
        }
    }
}
