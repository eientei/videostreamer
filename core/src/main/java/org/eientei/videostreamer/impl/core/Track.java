package org.eientei.videostreamer.impl.core;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public abstract class Track extends AbstractReferenceCounted{
    private final int timescale;
    private final int frametick;

    public Track(int timescale, int frametick) {
        this.timescale = timescale;
        this.frametick = frametick;
    }

    public int getTimescale() {
        return timescale;
    }

    public int getFrametick() {
        return frametick;
    }

    public abstract Sample makeSample(Message message) throws Exception;

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
