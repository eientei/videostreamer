package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class Sample extends AbstractReferenceCounted {
    private final Message message;
    private final int begin;
    private final int length;
    private final boolean key;

    public Sample(Message message, int begin, int length) {
        this.length = length;
        message.retain();
        this.message = message;
        this.begin = begin;

        int fst = message.getData().readUnsignedByte();
        key = ((fst & 0xf0) >> 4) == 1;
    }

    public Message getMessage() {
        return message;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }

    public int getBasetime() {
        return message.getHeader().getTime();
    }

    public boolean isKey() {
        return key;
    }

    public ByteBuf getData() {
        return message.getData(begin, length);
    }

    @Override
    protected void deallocate() {
        message.release();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
