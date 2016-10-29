package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class Mp4Sample {
    private final ByteBuf copydata;
    private final boolean keyframe;
    private final int timestamp;
    private final int duration;

    public Mp4Sample(ByteBuf copy, boolean isKeyframe, int timestamp, int frametick) {
        this.copydata = copy;
        this.keyframe = isKeyframe;
        this.timestamp = timestamp;
        this.duration = frametick;
    }

    public ByteBuf getCopydata() {
        return copydata;
    }

    public boolean isKeyframe() {
        return keyframe;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public double getDuration() {
        return duration;
    }

    public void release() {
        copydata.release();
    }
}
