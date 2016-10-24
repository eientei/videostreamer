package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Sample {
    private final ByteBuf data;
    private final boolean keyframe;
    private final int timestamp;
    private final int duration;

    public Mp4Sample(ByteBuf data, boolean keyframe, int timestamp, int duration) {
        this.data = data;
        this.keyframe = keyframe;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public ByteBuf getData() {
        return data;
    }

    public void release() {
        data.release();
    }

    public boolean isKeyframe() {
        return keyframe;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getDuration() {
        return duration;
    }
}
