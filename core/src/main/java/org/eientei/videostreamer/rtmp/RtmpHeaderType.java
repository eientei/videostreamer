package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public enum RtmpHeaderType {
    FULL(0),
    MEDIUM(1),
    SHORT(2),
    NONE(3);

    private final int value;

    RtmpHeaderType(int value) {
        this.value = value;
    }

    public static RtmpHeaderType dispatch(int value) {
        return RtmpHeaderType.values()[value & 0x3];
    }

    public int getValue() {
        return value;
    }
}
