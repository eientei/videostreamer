package org.eientei.videostreamer.ws;

/**
 * Created by Alexander Tumin on 2016-10-24
 */
public enum CommType {
    STREAM_PLAY(1),
    STREAM_STOP(2),
    STREAM_UPDATE_AV(3),
    STREAM_SUBSCRIBERS(4),
    STREAM_UPDATE_A(5),
    STREAM_UPDATE_V(6),
    STREAM_UPDATE_VK(7),
    STREAM_UPDATE_AVK(8),
    STREAM_UPDATE_AK(9);

    private final int num;

    CommType(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }
}
