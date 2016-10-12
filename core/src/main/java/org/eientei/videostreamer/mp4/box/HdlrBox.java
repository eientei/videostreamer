package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class HdlrBox extends FullBox {
    public HdlrBox(BoxContext context) {
        super("hdlr", context, 0, 0);
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0);
        out.writeBytes(getTrackTyped("vide", "soun").getBytes());
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeBytes(getTrackTyped("VideoHandler", "SoundHandler").getBytes());
        out.writeByte(0); // null-terminator
    }
}
