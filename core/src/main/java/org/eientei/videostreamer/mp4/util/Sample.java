package org.eientei.videostreamer.mp4.util;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Sample {
    private final byte[] mdatbuf;
    private final boolean key;
    private final int delay;
    private final boolean first;

    public Sample(byte[] mdatbuf, boolean key, int delay, boolean first) {
        this.mdatbuf = mdatbuf;
        this.key = key;
        this.delay = delay;
        this.first = first;
    }

    public byte[] getMdatbuf() {
        return mdatbuf;
    }

    public boolean isKey() {
        return key;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isFirst() {
        return first;
    }
}
