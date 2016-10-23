package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public abstract class Mp4Track {
    private final List<Mp4Sample> samples = new ArrayList<>();
    private final Mp4Context context;
    private final int volume;
    private final int width;
    private final int height;
    private final int timescale;
    private final int frametick;
    private Deque<List<Mp4Sample>> preps = new ArrayDeque<>();

    protected Mp4Track(Mp4Context context, int volume, int width, int height, int timescale, int frametick) {
        this.context = context;
        this.volume = volume;
        this.width = width;
        this.height = height;
        this.timescale = timescale;
        this.frametick = frametick;
    }

    public Mp4Context getContext() {
        return context;
    }

    public int id() {
        return context.getTracks().indexOf(this)+1;
    }

    public int getVolume() {
        return volume;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTimescale() {
        return timescale;
    }

    public int getFrametick() {
        return frametick;
    }

    public synchronized boolean isSamplesReady() {
        return !preps.isEmpty();
    }

    public synchronized void addSample(Mp4Sample sample) {
        samples.add(sample);
        if (samples.size() * frametick >= timescale) {
            preps.add(new ArrayList<>(samples));
            samples.clear();
        }
    }

    public synchronized List<Mp4Sample> drainSamples() {
        return preps.removeFirst();
    }

    public abstract void update(ByteBuf readonly, boolean keyframe);
    public abstract void release();
    public abstract String getShortHandler();
    public abstract String getLongHandler();
    public abstract Mp4Box getInit();
    public abstract Mp4Box getMhd();
}
