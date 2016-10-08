package org.eientei.videostreamer.util;

import com.github.jinahya.bit.io.BitInput;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public abstract class BitParser {
    protected BitInput in;


    public BitParser(BitInput in) {
        this.in = in;
    }


    protected int parseUE() throws IOException {
        int n = readN0();
        if (n == 0) {
            return 0;
        }
        return (int) (Math.pow(2,n) - 1 + in.readInt(true, n));
    }

    protected int parseSE() throws IOException {
        int n = readN0();
        if (n == 0) {
            return 0;
        }
        return (int) (Math.pow(2,n) - 1 + in.readInt(false, n));
    }

    private int readN0() throws IOException {
        int n = -1;
        for (int b = 0; b == 0; n++) {
            b = in.readInt(true, 1);
        }
        return n;
    }

    protected int parseInt(int siz) throws IOException {
        return (int) in.readLong(true, siz);
    }
}
