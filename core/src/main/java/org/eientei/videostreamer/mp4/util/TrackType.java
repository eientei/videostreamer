package org.eientei.videostreamer.mp4.util;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public enum TrackType {
    VIDEO(0),
    AUDIO(1);

    private int value;

    TrackType(int i) {
        value = i;
    }

    public int getValue() {
        return value;
    }
}

