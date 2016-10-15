package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Url_Box extends FullBox {
    public Url_Box(Mp4Context context) {
        super("url ", context, 0, 0x01);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
    }
}
