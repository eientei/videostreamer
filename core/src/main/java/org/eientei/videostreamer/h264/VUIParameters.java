package org.eientei.videostreamer.h264;

import com.github.jinahya.bit.io.BitInput;
import org.eientei.videostreamer.util.BitParser;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class VUIParameters extends BitParser {
    public final int aspectRatioInfoPresentFlag;
    public final int aspectRatioIdc;
    public final int sarWidth;
    public final int sarHeight;
    public final int overscanInfoPresentFlag;
    public final int overscanAppropriateFlag;
    public final int videoSignalTypePresentFlag;
    public final int videoFormat;
    public final int videoFullRangeFlag;
    public final int videoColourDescriptionPresentFlag;
    public final int colourPrimaries;
    public final int transferCharacteristics;
    public final int matrixCoefficent;
    public final int chromaLocInfoPresentFlag;
    public final int chromaSampleLocTypeTopField;
    public final int chromaSampleLocTypeBottomField;
    public final int timingInfoPresentFlag;
    public final int numUnitsInTick;
    public final int timeScale;
    public final int fixedFrameRateFlag;
    public final int nalHrdParametersPresentFlag;
    public final HrdParameters nalHrd;
    public final int vclHrdParamtersPresentFlag;
    public final HrdParameters vclHrd;
    public final int lowDelayHrdFlag;
    public final int picStructPresentFlag;
    public final int bitstreamRestrictionFlag;
    public final int motionVectorsOverPicBoundariesFlag;
    public final int maxBytesPerPicDenom;
    public final int maxBitsPeMbDenom;
    public final int log2MaxMvLengthHorizontal;
    public final int log2MaxMvLengthVertical;
    public final int numReorderFrames;
    public final int maxDecFrameBuffering;

    public VUIParameters(BitInput in) throws IOException {
        super(in);

        aspectRatioInfoPresentFlag = parseInt(1);
        if (aspectRatioInfoPresentFlag != 0) {
            aspectRatioIdc = parseInt(8);
            if (aspectRatioIdc == 0xFF) {
                sarWidth = parseInt(16);
                sarHeight = parseInt(16);
            } else {
                sarWidth = 0;
                sarHeight = 0;
            }
        } else {
            aspectRatioIdc = 0;
            sarWidth = 0;
            sarHeight = 0;
        }

        overscanInfoPresentFlag = parseInt(1);
        if (overscanInfoPresentFlag != 0) {
            overscanAppropriateFlag = parseInt(1);
        } else {
            overscanAppropriateFlag = 0;
        }

        videoSignalTypePresentFlag = parseInt(1);
        if (videoSignalTypePresentFlag != 0) {
            videoFormat = parseInt(3);
            videoFullRangeFlag = parseInt(1);
            videoColourDescriptionPresentFlag = parseInt(1);
            if (videoColourDescriptionPresentFlag != 0) {
                colourPrimaries = parseInt(8);
                transferCharacteristics = parseInt(8);
                matrixCoefficent = parseInt(8);
            } else {
                colourPrimaries = 0;
                transferCharacteristics = 0;
                matrixCoefficent = 0;
            }
        } else {
            videoFormat = 0;
            videoFullRangeFlag = 0;
            videoColourDescriptionPresentFlag = 0;

            colourPrimaries = 0;
            transferCharacteristics = 0;
            matrixCoefficent = 0;
        }

        chromaLocInfoPresentFlag = parseInt(1);
        if (chromaLocInfoPresentFlag != 0) {
            chromaSampleLocTypeTopField = parseUE();
            chromaSampleLocTypeBottomField = parseUE();
        } else {
            chromaSampleLocTypeTopField = 0;
            chromaSampleLocTypeBottomField = 0;
        }
        timingInfoPresentFlag = parseInt(1);
        if (timingInfoPresentFlag != 0) {
            numUnitsInTick = parseInt(32);
            timeScale = parseInt(32);
            fixedFrameRateFlag = parseInt(1);
        } else {
            numUnitsInTick = 0;
            timeScale = 0;
            fixedFrameRateFlag = 0;
        }

        nalHrdParametersPresentFlag = parseInt(1);
        if (nalHrdParametersPresentFlag != 0) {
            nalHrd = new HrdParameters(in);
        } else {
            nalHrd = null;
        }

        vclHrdParamtersPresentFlag = parseInt(1);
        if (vclHrdParamtersPresentFlag != 0) {
            vclHrd = new HrdParameters(in);
        } else {
            vclHrd = null;
        }

        if (nalHrd != null || vclHrd != null) {
            lowDelayHrdFlag = parseInt(1);
        } else {
            lowDelayHrdFlag = 0;
        }

        picStructPresentFlag = parseInt(1);
        bitstreamRestrictionFlag = parseInt(1);
        if (bitstreamRestrictionFlag != 0) {
            motionVectorsOverPicBoundariesFlag = parseInt(1);
            maxBytesPerPicDenom = parseUE();
            maxBitsPeMbDenom = parseUE();
            log2MaxMvLengthHorizontal = parseUE();
            log2MaxMvLengthVertical = parseUE();
            numReorderFrames = parseUE();
            maxDecFrameBuffering = parseUE();
        } else {
            motionVectorsOverPicBoundariesFlag = 0;
            maxBytesPerPicDenom = 0;
            maxBitsPeMbDenom = 0;
            log2MaxMvLengthHorizontal = 0;
            log2MaxMvLengthVertical = 0;
            numReorderFrames = 0;
            maxDecFrameBuffering = 0;
        }
    }
}
