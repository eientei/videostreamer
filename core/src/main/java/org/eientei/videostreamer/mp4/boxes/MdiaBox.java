package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class MdiaBox extends Box {
    private final MdhdBox mdhd;
    private final HdlrBox hdlr;
    private final MinfBox minf;

    public MdiaBox(Mp4Context context, Mp4Track track) {
        super("mdia", context);

        this.mdhd = new MdhdBox(context, track);
        this.hdlr = new HdlrBox(context, track);
        this.minf = new MinfBox(context, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mdhd.write(out);
        hdlr.write(out);
        minf.write(out);
    }
}
