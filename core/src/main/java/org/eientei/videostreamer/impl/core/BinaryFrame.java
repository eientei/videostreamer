package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import org.eientei.videostreamer.impl.mp4.Box;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class BinaryFrame extends AbstractReferenceCounted {
    private final ByteBuf p1;
    private final ByteBuf p2;
    private final ByteBuf p3;
    private final int audioTick;
    private final int videoTick;
    private final int audioAdvance;
    private final int videoAdvance;

    private final ByteBuf data;
    private final boolean key;

    public BinaryFrame(ByteBufAllocator alloc, Frame frame) {
        data = alloc.heapBuffer();

        int[] offsets = Box.moof(data, frame);
        Box.mdat(data, frame);

        audioAdvance = frame.getAudioAdvance();
        videoAdvance = frame.getVideoAdvance();

        audioTick = frame.getAudioList().getFrametick();
        videoTick = frame.getVideoList().getFrametick();

        p1 = data.slice(0, offsets[0]);
        p2 = data.slice(offsets[0]+4, offsets[1] - offsets[0] - 4);
        p3 = data.slice(offsets[1]+4, data.readableBytes() - offsets[1] - 4);

        key = frame.isKey();
    }

    public boolean isKey() {
        return key;
    }

    public ByteBuf getP1() {
        return p1;
    }

    public ByteBuf getP2() {
        return p2;
    }

    public ByteBuf getP3() {
        return p3;
    }

    public int getAudioTime(int num) {
        return num * audioTick;
    }

    public int getVideoTime(int num) {
        return num * videoTick;
    }

    public int getAudioAdvance() {
        return audioAdvance;
    }

    public int getVideoAdvance() {
        return videoAdvance;
    }

    @Override
    protected void deallocate() {
        data.release();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
