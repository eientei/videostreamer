package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class Header {
    public enum Type {
        FULL(0),
        MEDIUM(1),
        SMALL(2),
        NONE(3);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final int chunk;
    private final int time;
    private final int length;
    private final Message.Type type;
    private final int stream;

    private Header(int chunk, int time, int length, Message.Type type, int stream) {
        if (chunk < 2) {
            throw new IllegalArgumentException("Chunkstream IDs < 2 are reserved");
        }
        this.chunk = chunk;
        this.time = time;
        this.length = length;
        this.type = type;
        this.stream = stream;
    }

    public Header(int chunk, int time, Message.Type type, int stream) {
        this(chunk, time, -1, type, stream);
    }

    public Header(Header header, ByteBuf data) {
        this(header.getChunk(), header.getTime(), data.readableBytes(), header.getType(), header.getStream());
    }

    public int getChunk() {
        return chunk;
    }

    public int getTime() {
        return time;
    }

    public int getLength() {
        return length;
    }

    public Message.Type getType() {
        return type;
    }

    public int getStream() {
        return stream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return chunk == header.chunk &&
                time == header.time &&
                stream == header.stream &&
                type == header.type;
    }
}
