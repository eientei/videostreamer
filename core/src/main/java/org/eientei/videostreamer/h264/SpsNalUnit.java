package org.eientei.videostreamer.h264;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class SpsNalUnit extends NalUnit {
    public final int profileIdc;
    public final int constraintSet0Flag;
    public final int constraintSet1Flag;
    public final int constraintSet2Flag;
    public final int reservedZero5Bits;
    public final int levelIdc;
    public final int seqParameterSet;
    public final int log2MaxFrameNumMinus4;
    public final int picOrderCntType;
    public final int log2MaxPicOrderCntLsbMinus4;
    public final int deltaPicOrderAlwaysZero;
    public final int offsetForNonRefPic;
    public final int offsetForTopToBottomField;
    public final int numRefFramesInPicOrderCntCycle;
    public final int[] offsetForRefFrame;
    public final int numrefFrames;
    public final int gapsInFrameNumValueAllowedFlag;
    public final int picWidthInMbsMinusOne;
    public final int picHeightInMapUnitsMinusOne;
    public final int frameMbsOnlyFlag;
    public final int mbAdaptiveFrameFieldFlag;
    public final int dirct8x8InterferenceFlag;
    public final int frameCroppingFlag;
    public final int framecropLeftOffset;
    public final int framecropRightOffset;
    public final int framecropTopOffset;
    public final int framecropBottomOffset;
    public final int vuiParametersPresentFlag;
    public final VUIParameters vui;

    public SpsNalUnit(ByteBuffer buf) throws IOException {
        super(buf);
        profileIdc = parseInt(8);
        constraintSet0Flag = parseInt(1);
        constraintSet1Flag = parseInt(1);
        constraintSet2Flag = parseInt(1);
        reservedZero5Bits = parseInt(5);
        levelIdc = parseInt(8);
        seqParameterSet = parseUE();
        log2MaxFrameNumMinus4 = parseUE();
        picOrderCntType = parseUE();
        if (picOrderCntType == 0) {
            log2MaxPicOrderCntLsbMinus4 = parseUE();

            deltaPicOrderAlwaysZero = 0;
            offsetForNonRefPic = 0;
            offsetForTopToBottomField = 0;
            numRefFramesInPicOrderCntCycle = 0;
            offsetForRefFrame = new int[0];
        } else if (picOrderCntType == 1) {
            log2MaxPicOrderCntLsbMinus4 = 0;

            deltaPicOrderAlwaysZero = parseInt(1);
            offsetForNonRefPic = parseSE();
            offsetForTopToBottomField = parseSE();
            numRefFramesInPicOrderCntCycle = parseUE();
            offsetForRefFrame = new int[numRefFramesInPicOrderCntCycle];
            for (int i = 0; i < numRefFramesInPicOrderCntCycle; i++) {
                offsetForRefFrame[i] = parseSE();
            }
        } else {
            log2MaxPicOrderCntLsbMinus4 = 0;

            deltaPicOrderAlwaysZero = 0;
            offsetForNonRefPic = 0;
            offsetForTopToBottomField = 0;
            numRefFramesInPicOrderCntCycle = 0;
            offsetForRefFrame = new int[0];
        }
        numrefFrames = parseUE();
        gapsInFrameNumValueAllowedFlag = parseInt(1);
        picWidthInMbsMinusOne = parseUE();
        picHeightInMapUnitsMinusOne = parseUE();
        frameMbsOnlyFlag = parseInt(1);
        if (frameMbsOnlyFlag == 0) {
            mbAdaptiveFrameFieldFlag = parseInt(1);
        } else {
            mbAdaptiveFrameFieldFlag = 0;
        }

        dirct8x8InterferenceFlag = parseInt(1);
        frameCroppingFlag = parseInt(1);
        if (frameCroppingFlag != 0) {
            framecropLeftOffset = parseUE();
            framecropRightOffset = parseUE();
            framecropTopOffset = parseUE();
            framecropBottomOffset = parseUE();
        } else {
            framecropLeftOffset = 0;
            framecropRightOffset = 0;
            framecropTopOffset = 0;
            framecropBottomOffset = 0;
        }

        vuiParametersPresentFlag = parseInt(1);
        if (vuiParametersPresentFlag != 0) {
            vui = new VUIParameters(in);
        } else {
            vui = null;
        }
    }
}
