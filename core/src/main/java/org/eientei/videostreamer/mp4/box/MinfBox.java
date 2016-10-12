package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MinfBox extends Box {
    private final VmhdBox vmhd;
    private final SmhdBox smhd;
    private final DinfBox dinf;
    private final StblBox stbl;

    public MinfBox(BoxContext context) {
        super("minf", context);
        vmhd = new VmhdBox(context);
        smhd = new SmhdBox(context);
        dinf = new DinfBox(context);
        stbl = new StblBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        getTrackTyped(vmhd, smhd).write(out);
        dinf.write(out);
        stbl.write(out);
    }
}
