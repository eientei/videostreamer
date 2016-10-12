package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class DinfBox extends Box {
    private final DrefBox dref;

    public DinfBox(BoxContext context) {
        super("dinf", context);
        dref = new DrefBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        dref.write(out);
    }
}
