package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.ws.CommType;

import java.util.*;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public abstract class Mp4Track {
    private final Deque<Mp4Sample> samples = new LinkedList<>();
    protected final Mp4Context context;
    private final int volume;
    private final int width;
    private final int height;
    private final double timescale;
    private final double frametick;
    private Deque<List<Mp4Sample>> preps = new ArrayDeque<>();
    protected double seq = 0;

    protected Mp4Track(Mp4Context context, int volume, int width, int height, double timescale, double frametick) {
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

    public int id(List<Mp4Track> tracks) {
        if (tracks.size() == 1) {
            return 1;
        } else {
            return tracks.indexOf(this)+1;
        }
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

    public double getTimescale() {
        return timescale;
    }

    public double getFrametick() {
        return frametick;
    }

    public synchronized boolean isSamplesReady() {
        return samples.size() > 0;
    }

    public synchronized void addSample(Mp4Sample sample) {
        samples.add(sample);
        /*
        if (samples.size() * frametick >= timescale) {
            preps.add(new ArrayList<>(samples));
            samples.clear();
            seq += getStart();
        }
        */
    }

    public synchronized List<Mp4Sample> drainSamples() {
        List<Mp4Sample> ss = new ArrayList<>(samples);
        samples.clear();
        seq += getStart();
        return ss;
        //return preps.removeFirst();
    }

    public abstract void update(ByteBuf readonly, int timestamp, boolean keyframe);
    public abstract void release();
    public abstract String getShortHandler();
    public abstract String getLongHandler();
    public abstract Mp4Box getInit(List<Mp4Track> tracks);
    public abstract Mp4Box getMhd();
    public abstract CommType getType(Mp4Frame frame);
    public abstract double getStart();
    public abstract int getFix();
}
