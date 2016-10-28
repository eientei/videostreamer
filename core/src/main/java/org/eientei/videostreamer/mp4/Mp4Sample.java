package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Sample {
    private final ByteBuf data;
    private final boolean keyframe;
    private final double timestamp;
    private final double duration;

    public Mp4Sample(ByteBuf data, boolean keyframe, double timestamp, double duration) {
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

    public double getTimestamp() {
        return timestamp;
    }

    public double getDuration() {
        return duration;
    }
}
