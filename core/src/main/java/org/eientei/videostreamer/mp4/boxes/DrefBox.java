package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class DrefBox extends FullBox {
    private final Url_Box url_;

    public DrefBox(Mp4Context context) {
        super("dref", context, 0, 0);
        this.url_ = new Url_Box(context);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(1);
        url_.write(out);
    }
}
