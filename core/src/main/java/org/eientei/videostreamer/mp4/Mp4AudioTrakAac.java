package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.boxes.Mp4Mp4aBox;
import org.eientei.videostreamer.mp4.boxes.Mp4SmhdBox;
import org.eientei.videostreamer.ws.CommType;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4AudioTrakAac extends Mp4Track {
    private final int channels;
    private final int sampleSize;
    private final int sampleRate;
    private final ByteBuf aac;
    private final Mp4Box mhd;

    public Mp4AudioTrakAac(Mp4Context context, int channels, int sampleSize, int sampleRate, int sampleCount, ByteBuf audioro) {
        super(context, 1, 0, 0, 1000000.0, 1000000.0 / ((double)sampleRate / (double)sampleCount));
        this.channels = channels;
        this.sampleSize = sampleSize;
        this.sampleRate = sampleRate;
        this.aac = audioro.copy(audioro.readerIndex(), 2);
        this.mhd = new Mp4SmhdBox(context);
    }

    @Override
    public void update(ByteBuf readonly, int timestamp, boolean keyframe) {
        addSample(new Mp4Sample(readonly.copy(), keyframe, timestamp, getFrametick()));
    }

    @Override
    public void release() {
        aac.release();
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
    public Mp4Box getMhd() {
        return mhd;
    }

    @Override
    public CommType getType(Mp4Frame frame) {
        if (frame == null) {

            return CommType.STREAM_UPDATE_AK;
        }
        return CommType.STREAM_UPDATE_A;
    }

    @Override
    public double getStart() {
            return 1.0;
        //return 0;
    }

    @Override
    public int getFix() {
        return 0;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public ByteBuf getAac() {
        return aac.slice();
    }
}
