package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.aac.AacHeader;
import org.eientei.videostreamer.mp4.boxes.Mp4aBox;
import org.eientei.videostreamer.mp4.boxes.SmhdBox;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Alexander Tumin on 2016-10-15
 */
public class Mp4AudioTrack extends Mp4Track {
    public AacHeader aac;
    public List<Mp4Sample> samples = new ArrayList<>();
    public final ByteBuf spec;
    public int codec;
    public int channels;
    public int samplesiz;
    public int samplerate;
    private Queue<Mp4TrackFrame> frames = new LinkedBlockingQueue<>();
    private int preticks;

    public Mp4AudioTrack(Mp4Context context, ByteBuf spec) {
        super(context, "soun", "SoundHandler", new SmhdBox(context));
        this.spec = spec;
        init = new Mp4aBox(context, this);
    }

    @Override
    public Mp4TrackFrame getFrame() {
        return frames.poll();
    }

    public void addSample(Mp4AudioSample sample) {
        //if (samples.size() * frametick >= timescale) {
        //if (samples.size() >= 3) {
        //preticks += 576;
        //if (preticks >= 22050/24) {
        //if (samples.size() * frametick >= timescale) {
        //}

        if (samples.size() * frametick >= timescale) {
            Mp4TrackFrame frame = createFrame();
            frames.add(frame);
            //System.out.println(frames.size());
        }
        preticks += frametick;
        samples.add(sample);
    }

    private Mp4TrackFrame createFrame() {
        Mp4TrackFrame mp4 = new Mp4TrackFrame(this, samples);
        samples.clear();
        ticks = preticks;
        return mp4;
    }

    @Override
    public boolean isCompleteFrame() {
        return !frames.isEmpty() && context.inited;
    }
}
