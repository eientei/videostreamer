package org.eientei.videostreamer.h264;

import com.github.jinahya.bit.io.BufferByteInput;
import com.github.jinahya.bit.io.DefaultBitInput;
import org.eientei.videostreamer.util.BitParser;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class NalUnit extends BitParser {
    public final int nalUnitType;
    public final int nalRefIdc;
    public final int forbiddenZeroBit;

    public NalUnit(ByteBuffer buf) throws IOException {
        super(new DefaultBitInput<>(new BufferByteInput(buf)));
        forbiddenZeroBit = parseInt(1);
        nalRefIdc = parseInt(2);
        nalUnitType = parseInt(5);
    }
}
