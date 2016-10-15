package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class DinfBox extends Box {
    private final DrefBox dref;

    public DinfBox(Mp4Context context) {
        super("dinf", context);
        this.dref = new DrefBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        dref.write(out);
    }
}
