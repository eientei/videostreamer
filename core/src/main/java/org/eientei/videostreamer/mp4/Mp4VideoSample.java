package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.h264.SliceNalUnit;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4VideoSample extends Mp4Sample {
    private final SliceNalUnit slice;

    public Mp4VideoSample(SliceNalUnit slice, ByteBuf naldata) {
        super(naldata);
        this.slice = slice;
    }

    public SliceNalUnit getSlice() {
        return slice;
    }
}
