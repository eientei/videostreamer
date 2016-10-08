package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Sample {
    private final boolean key;
    private final int delay;
    private final int size;
    private final int duration;
    private final MetaData meta;

    public Sample(MetaData meta, int length) {
        this.meta = meta;
        this.duration = meta.getFrameTick();
        this.key = true;
        this.delay = 0;
        this.size = length;
    }


    public boolean isKey() {
        return key;
    }

    public int getDelay() {
        return delay;
    }

    public int getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }

    public MetaData getMeta() {
        return meta;
    }
}
