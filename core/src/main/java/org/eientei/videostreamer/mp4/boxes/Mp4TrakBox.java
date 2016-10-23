package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4TrakBox extends Mp4Box {
    private final Mp4TkhdBox tkhd;
    private final Mp4MdiaBox mdia;

    public Mp4TrakBox(Mp4Context context, Mp4Track track) {
        super("trak", context);
        this.tkhd = new Mp4TkhdBox(context, track);
        this.mdia = new Mp4MdiaBox(context, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        tkhd.write(out);
        mdia.write(out);
    }
}

