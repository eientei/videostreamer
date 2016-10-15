package org.eientei.videostreamer.h264;

import com.github.jinahya.bit.io.BitInput;
import org.eientei.videostreamer.util.BitParser;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class HrdParameters extends BitParser {
    public final int cpbCntMinusOne;
    public final int bitRateScale;
    public final int cpbSizeScale;
    public final int[] bitRateValueMinusOne;
    public final int[] cpbSizeValueMinusOne;
    public final int[] cbrFlag;
    public final int initialCpbRemovalDelayLengthOne;
    public final int cpbRemovalDelaylengthMinusOne;
    public final int dpbOutputDelayLengthMinusOne;
    public final int timeOffsetLength;

    public HrdParameters(BitInput in) {
        super(in);
        cpbCntMinusOne = parseUE();
        bitRateScale = parseInt(4);
        cpbSizeScale = parseInt(4);
        bitRateValueMinusOne = new int[cpbCntMinusOne+1];
        cpbSizeValueMinusOne = new int[cpbCntMinusOne+1];
        cbrFlag = new int[cpbCntMinusOne+1];
        for (int i = 0; i <= cpbCntMinusOne; i++) {
            bitRateValueMinusOne[i] = parseUE();
            cpbSizeValueMinusOne[i] = parseUE();
            cbrFlag[i] = parseInt(1);
        }

        initialCpbRemovalDelayLengthOne = parseInt(5);
        cpbRemovalDelaylengthMinusOne = parseInt(5);
        dpbOutputDelayLengthMinusOne = parseInt(5);
        timeOffsetLength = parseInt(5);
    }
}
