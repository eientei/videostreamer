package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MdiaBox extends Mp4Box {
    private final Mp4MdhdBox mdhd;
    private final Mp4HdlrBox hdlr;
    private final Mp4MinfBox minf;

    public Mp4MdiaBox(Mp4Context context, List<Mp4Track> tracks, Mp4Track track) {
        super("mdia", context);
        this.mdhd = new Mp4MdhdBox(context, track);
        this.hdlr = new Mp4HdlrBox(context, track);
        this.minf = new Mp4MinfBox(context, tracks, track);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mdhd.write(out);
        hdlr.write(out);
        minf.write(out);
    }
}
