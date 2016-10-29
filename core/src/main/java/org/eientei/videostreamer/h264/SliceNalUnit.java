package org.eientei.videostreamer.h264;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class SliceNalUnit extends NalUnit {
    public final int firstMbInSlice;
    public final int sliceType;
    public final int picParameterSetId;
    public final int frameNum;

    public SliceNalUnit(SpsNalUnit sps, ByteBuf buf) {
        super(buf);
        firstMbInSlice = parseUE();
        sliceType = parseUE();
        picParameterSetId = parseUE();
        frameNum = parseInt(sps.log2MaxFrameNumMinus4+4);
    }
}
