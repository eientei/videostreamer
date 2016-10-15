package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.h264.SliceNalUnit;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4VideoSample {
    private final SliceNalUnit slice;
    private final ByteBuf naldata;

    public Mp4VideoSample(SliceNalUnit slice, ByteBuf naldata) {
        this.slice = slice;
        this.naldata = naldata;
    }

    public SliceNalUnit getSlice() {
        return slice;
    }

    public ByteBuf getNaldata() {
        return naldata;
    }

    public void dispose() {
        naldata.release();
    }
}
