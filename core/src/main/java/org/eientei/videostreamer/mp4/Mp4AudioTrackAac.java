package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.boxes.Mp4Mp4aBox;
import org.eientei.videostreamer.mp4.boxes.Mp4SmhdBox;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class Mp4AudioTrackAac extends Mp4Track {
    private final Mp4RemuxerHandler context;
    private final int channels;
    private final int sampleSize;
    private final int sampleRate;
    private final ByteBuf aac;
    private final Mp4SmhdBox mhd;

    public Mp4AudioTrackAac(Mp4RemuxerHandler context, int channels, int sampleSize, int sampleRate, int sampleCount, ByteBuf audioro) {
        super(1, 0, 0, 1000, 1000 / (sampleRate / sampleCount));
        this.context = context;
        this.channels = channels;
        this.sampleSize = sampleSize;
        this.sampleRate = sampleRate;
        this.aac = audioro.copy();
        this.mhd = new Mp4SmhdBox(context);
    }

    public ByteBuf getAac() {
        return aac.slice();
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getChannels() {
        return channels;
    }

    @Override
    public void update(ByteBuf data, int timestamp, boolean isKeyframe) {
        addSample(new Mp4Sample(data.copy(), isKeyframe, timestamp, getFrametick()));
    }

    @Override
    public String getShortHandler() {
        return "soun";
    }

    @Override
    public String getLongHandler() {
        return "Sound Handler";
    }

    @Override
    public Mp4Box getInit(List<Mp4Track> tracks) {
        return new Mp4Mp4aBox(context, tracks, this);
    }

    @Override
    public Mp4Box get_Mhd() {
        return mhd;
    }

    @Override
    public void release() {
        aac.release();
        for (Mp4Sample sample : drainSamples()) {
            sample.release();
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
