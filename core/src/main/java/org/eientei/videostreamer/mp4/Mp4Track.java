package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public abstract class Mp4Track {
    private final Deque<Mp4Sample> samples = new LinkedList<>();

    private final int volume;
    private final int width;
    private final int height;
    private final int timescale;
    private final int frametick;


    public Mp4Track(int volume, int width, int height, int timescale, int frametick) {
        this.volume = volume;
        this.width = width;
        this.height = height;
        this.timescale = timescale;
        this.frametick = frametick;
    }

    public int getId(List<Mp4Track> tracks) {
        return tracks.indexOf(this)+1;
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

    public boolean isSamplesReady() {
        return !samples.isEmpty();
    }

    protected void addSample(Mp4Sample sample) {
        samples.add(sample);
    }

    public List<Mp4Sample> drainSamples() {
        List<Mp4Sample> ss = new ArrayList<>(samples);
        samples.clear();
        return ss;
    }

    public abstract void update(ByteBuf data, int timestamp, boolean isKeyframe);
    public abstract String getShortHandler();
    public abstract String getLongHandler();
    public abstract Mp4Box getInit(List<Mp4Track> tracks);
    public abstract Mp4Box get_Mhd();
    public abstract void release();

}
