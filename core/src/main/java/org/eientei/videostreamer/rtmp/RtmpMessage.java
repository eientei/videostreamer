package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public abstract class RtmpMessage {
    private final RtmpMessageType type;
    private final int chunk;
    private final int stream;
    private final int time;
    private final ByteBuf data;

    public RtmpMessage(RtmpMessageType type, int chunk, int stream, int time, ByteBuf data) {
        this.type = type;
        this.chunk = chunk;
        this.stream = stream;
        this.time = time;
        this.data = data;
    }

    public int getChunk() {
        return chunk;
    }

    public int getStream() {
        return stream;
    }

    public int getTime() {
        return time;
    }

    public ByteBuf getData() {
        return data;
    }

    public RtmpMessageType getType() {
        return type;
    }
}
