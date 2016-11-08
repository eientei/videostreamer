package org.eientei.videostreamer.impl.core;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class SampleList extends AbstractReferenceCounted {
    private final List<Sample> samples = new ArrayList<>();
    private final int earliest;
    private final int lattiest;
    private final int frametick;
    private final int size;
    private final boolean key;

    public SampleList(int frametick, List<Sample> samples) {
        this.frametick = frametick;
        this.samples.addAll(samples);
        this.earliest = samples.get(0).getBasetime();
        this.lattiest = samples.get(samples.size()-1).getBasetime();
        int size = 0;
        boolean isKey = false;
        for (Sample sample : samples) {
            size += sample.getData().readableBytes();
            isKey = isKey || sample.isKey();
        }
        this.key = isKey;
        this.size = size;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public int getEarliest() {
        return earliest;
    }

    public int getLattiest() {
        return lattiest;
    }

    public int getDuration() {
        return samples.size() * frametick;
    }

    public int getTotalSize() {
        return size;
    }

    public int getFrametick() {
        return frametick;
    }

    public boolean isKey() {
        return key;
    }

    @Override
    protected void deallocate() {
        for (Sample sample : samples) {
            sample.release();
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
