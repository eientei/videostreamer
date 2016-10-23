package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Sample {
    private final ByteBuf data;
    private final boolean keyframe;

    public Mp4Sample(ByteBuf data, boolean keyframe) {
        this.data = data;
        this.keyframe = keyframe;
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
}
