package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public enum RtmpHeaderSize {
    FULL(0),
    MEDIUM(1),
    SHORT(2),
    NONE(3);

    private final int value;

    RtmpHeaderSize(int value) {
        this.value = value;
    }

    public static RtmpHeaderSize dispatch(int value) {
        return RtmpHeaderSize.values()[value & 0x3];
    }

    public int getValue() {
        return value;
    }
}
