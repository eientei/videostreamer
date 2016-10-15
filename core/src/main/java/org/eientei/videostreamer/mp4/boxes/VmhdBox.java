package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.FullBox;
import org.eientei.videostreamer.mp4.Mp4Context;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class VmhdBox extends FullBox {
    public VmhdBox(Mp4Context context) {
        super("vmhd", context, 0, 0x01);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeShort(0); // graphic mode

        out.writeShort(0); // red
        out.writeShort(0); // green
        out.writeShort(0); // blue
    }
}
