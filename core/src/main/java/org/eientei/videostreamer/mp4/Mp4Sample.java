package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-15
 */
public class Mp4Sample {
    private final ByteBuf data;

    public Mp4Sample(ByteBuf data) {
        this.data = data;
    }

    public ByteBuf getData() {
        return data;
    }

    public void dispose() {
        data.release();
    }
}
