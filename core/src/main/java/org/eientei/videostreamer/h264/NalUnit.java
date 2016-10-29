package org.eientei.videostreamer.h264;

import com.github.jinahya.bit.io.DefaultBitInput;
import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.util.BitParser;
import org.eientei.videostreamer.util.ByteBufInput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class NalUnit extends BitParser {
    public final int nalUnitType;
    public final int nalRefIdc;
    public final int forbiddenZeroBit;

    public NalUnit(ByteBuf buf) {
        super(new DefaultBitInput<>(new ByteBufInput(buf.slice())));
        forbiddenZeroBit = parseInt(1);
        nalRefIdc = parseInt(2);
        nalUnitType = parseInt(5);
    }
}
