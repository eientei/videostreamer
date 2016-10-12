package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MvexBox extends Box {
    private final TrexBox trex;

    public MvexBox(BoxContext context) {
        super("mvex", context);
        trex = new TrexBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        trex.write(out);
    }
}
