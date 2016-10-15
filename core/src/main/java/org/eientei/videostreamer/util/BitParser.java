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


    protected int parseUE() {
        int n = readN0();
        if (n == 0) {
            return 0;
        }
        try {
            return (int) (Math.pow(2,n) - 1 + in.readInt(true, n));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected int parseSE() {
        int n = readN0();
        if (n == 0) {
            return 0;
        }
        try {
            return (int) (Math.pow(2,n) - 1 + in.readInt(false, n));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int readN0() {
        int n = -1;
        for (int b = 0; b == 0; n++) {
            try {
                b = in.readInt(true, 1);
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }
        return n;
    }

    protected int parseInt(int siz) {
        try {
            return (int) in.readLong(true, siz);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
