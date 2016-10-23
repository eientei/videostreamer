package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4DrefBox extends Mp4BoxFull {
    private final Mp4Url_Box url_;

    public Mp4DrefBox(Mp4Context context) {
        super("dref", context, 0, 0);
        this.url_ = new Mp4Url_Box(context);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1);
        url_.write(out);
    }
}
