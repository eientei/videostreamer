package org.eientei.videostreamer.h264;

import com.github.jinahya.bit.io.ArrayByteInput;
import com.github.jinahya.bit.io.DefaultBitInput;
import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.util.BitParser;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class NalUnit extends BitParser {
    public final int nalUnitType;
    public final int nalRefIdc;
    public final int forbiddenZeroBit;

    public NalUnit(ByteBuf buf, int length) {
        super(new DefaultBitInput<>(new ArrayByteInput(buf.copy().array(), 0, length)));
        forbiddenZeroBit = parseInt(1);
        nalRefIdc = parseInt(2);
        nalUnitType = parseInt(5);
    }
}
