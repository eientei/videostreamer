package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4DinfBox extends Mp4Box {
    private final Mp4DrefBox dref;

    public Mp4DinfBox(Mp4RemuxerHandler context) {
        super("dinf", context);
        this.dref = new Mp4DrefBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        dref.write(out);
    }
}
