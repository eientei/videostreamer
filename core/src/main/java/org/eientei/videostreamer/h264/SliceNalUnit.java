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

    public SliceNalUnit(SpsNalUnit sps, ByteBuf buf, int slicelength) {
        super(buf, slicelength);
        firstMbInSlice = parseUE();
        sliceType = parseUE();
        picParameterSetId = parseUE();
        frameNum = parseInt(sps.log2MaxFrameNumMinus4+4);
    }

    public SliceNalUnit(ByteBuf buf, int slicelength) {
        super(buf, slicelength);
        firstMbInSlice = parseUE();
        sliceType = parseUE();
        picParameterSetId = parseUE();
        frameNum = 0;
    }
}
