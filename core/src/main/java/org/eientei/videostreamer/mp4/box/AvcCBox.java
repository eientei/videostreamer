package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class AvcCBox extends Box {
    public AvcCBox(BoxContext context) {
        super("avcC", context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        ByteBuf bb = context.get(Type.VIDEO_AVC_DATA);
        bb.resetReaderIndex();
        out.writeBytes(bb);
    }
}
