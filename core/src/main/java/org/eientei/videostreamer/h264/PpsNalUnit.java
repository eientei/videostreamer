package org.eientei.videostreamer.h264;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class PpsNalUnit extends NalUnit {
    public final int picParameterSetId;
    public final int seqParameterSetId;
    public final int entropyCodingModeFlag;
    public final int picOrderPresentFlag;
    public final int numSliceGroupMinusOne;
    public final int sliceGroupMapType;
    public final List<Integer> runLengthMinusOne = new ArrayList<>();
    public final List<Integer> topLeft = new ArrayList<>();
    public final List<Integer> bottomRight = new ArrayList<>();
    public final int sliceGroupChangeDirectionFlag;
    public final int sliceGroupChangeRateMinusOne;
    public final int picSizeInMapUnitsMinusOne;
    public final List<Integer> sliceGroupId = new ArrayList<>();
    public final int numRefIdx10ActiveMinusOne;
    public final int numRefIdx11ActiveMinusOne;
    public final int weightedPredFlag;
    public final int weightedBipredIdc;
    public final int picUnitQpMinus26;
    public final int picUnitQsMinus26;
    public final int chromaQpIndexOffset;
    public final int deblockingFilterControlpresentFlag;
    public final int constrainedIntrapredFlag;
    public final int redundatPicCntPresentFlag;

    public PpsNalUnit(ByteBuf buf) {
        super(buf);
        picParameterSetId = parseUE();
        seqParameterSetId = parseUE();
        entropyCodingModeFlag = parseInt(1);
        picOrderPresentFlag = parseInt(1);
        numSliceGroupMinusOne = parseUE();
        if (numSliceGroupMinusOne > 0) {
            sliceGroupMapType = parseUE();
            if (sliceGroupMapType == 0) {
                for (int i = 0; i <= numSliceGroupMinusOne; i++) {
                    runLengthMinusOne.add(parseUE());
                }
                sliceGroupChangeDirectionFlag = -1;
                sliceGroupChangeRateMinusOne = -1;
                picSizeInMapUnitsMinusOne = -1;
            } else if (sliceGroupMapType == 2) {
                for (int i = 0; i < numSliceGroupMinusOne; i++) {
                    topLeft.add(parseUE());
                    bottomRight.add(parseUE());
                }
                sliceGroupChangeDirectionFlag = -1;
                sliceGroupChangeRateMinusOne = -1;
                picSizeInMapUnitsMinusOne = -1;
            } else if (sliceGroupMapType == 3  || sliceGroupMapType == 4 || sliceGroupMapType == 5) {
                sliceGroupChangeDirectionFlag = parseInt(1);
                sliceGroupChangeRateMinusOne = parseUE();
                picSizeInMapUnitsMinusOne = -1;
            } else if (sliceGroupMapType == 6) {
                int nbits;
                if (numSliceGroupMinusOne + 1 > 4) {
                    nbits = 3;
                } else if (numSliceGroupMinusOne + 1 > 2) {
                    nbits = 2;
                } else {
                    nbits = 1;
                }
                picSizeInMapUnitsMinusOne = parseUE();
                for (int i = 0; i <= picSizeInMapUnitsMinusOne; i++) {
                    sliceGroupId.add(parseInt(nbits));
                }
                sliceGroupChangeDirectionFlag = -1;
                sliceGroupChangeRateMinusOne = -1;
            } else {
                sliceGroupChangeDirectionFlag = -1;
                sliceGroupChangeRateMinusOne = -1;
                picSizeInMapUnitsMinusOne = -1;
            }
        } else {
            sliceGroupMapType = -1;
            sliceGroupChangeDirectionFlag = -1;
            sliceGroupChangeRateMinusOne = -1;
            picSizeInMapUnitsMinusOne = -1;
        }

        numRefIdx10ActiveMinusOne = parseUE();
        numRefIdx11ActiveMinusOne = parseUE();
        weightedPredFlag = parseInt(1);
        weightedBipredIdc = parseInt(2);
        picUnitQpMinus26 = parseSE();
        picUnitQsMinus26 = parseSE();
        chromaQpIndexOffset = parseSE();
        deblockingFilterControlpresentFlag = parseInt(1);
        constrainedIntrapredFlag = parseInt(1);
        redundatPicCntPresentFlag = parseInt(1);
    }
}
