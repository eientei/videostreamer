package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public enum Type {
    VIDEO(0),
    AUDIO(1);

    private int value;

    Type(int i) {
        value = i;
    }

    public int getValue() {
        return value;
    }
}

