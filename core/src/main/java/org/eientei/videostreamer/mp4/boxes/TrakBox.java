package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class TrakBox extends Box {
    private final TkhdBox tkhd;
    private final MdiaBox mdia;

    public TrakBox(Mp4Context context, Mp4Track track) {
        super("trak", context);
        this.tkhd = new TkhdBox(context, track);
        this.mdia = new MdiaBox(context, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tkhd.write(out);
        mdia.write(out);
    }
}
