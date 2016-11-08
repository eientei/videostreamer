package org.eientei.videostreamer.impl.tracks;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.Sample;
import org.eientei.videostreamer.impl.core.Track;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class TrackAudioAac extends Track implements TrackAudio {
    private final int channels;
    private final Message init;
    private final int sampleSize;
    private final int sampleRate;

    public TrackAudioAac(Message audio, int sampleRate, int sampleCount) {
        super(sampleRate, sampleCount);

        int fstsnd = audio.getData().getByte(0);
        this.channels = (fstsnd & 0x01) + 1;
        this.sampleSize = ((fstsnd & 0x02) != 0) ? 32 : 16;
        this.sampleRate = sampleRate;

        audio.retain();
        init = audio;
    }

    @Override
    protected void deallocate() {
        init.release();
    }

    public int getChannels() {
        return channels;
    }

    @Override
    public int getSampleSize() {
        return sampleSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public ByteBuf getInit() {
        return init.getData().skipBytes(2);
    }

    @Override
    public Sample makeSample(Message message) throws Exception {
        ByteBuf data = message.getData();
        data.skipBytes(2);
        return new Sample(message, data.readerIndex(), data.readableBytes());
    }
}
