package org.eientei.videostreamer.ws;

/**
 * Created by Alexander Tumin on 2016-10-24
 */
public enum CommType {
    STREAM_PLAY(1),
    STREAM_STOP(2),
    STREAM_SUBSCRIBERS(3),
    STREAM_UPDATE_AV(4),
    STREAM_UPDATE_AVK(5);

    private final int num;

    CommType(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }
}
