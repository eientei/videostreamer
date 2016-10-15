package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class SmhdBox extends FullBox {
    public SmhdBox(Mp4Context context) {
        super("smhd", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeShort(0); // balance
        out.writeShort(0); // reserved
    }
}
