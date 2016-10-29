package org.eientei.videostreamer.mp4;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class SamplesPack {
    private final Mp4Track track;
    private final List<Mp4Sample> samples;

    public SamplesPack(Mp4Track track, List<Mp4Sample> samples) {
        this.track = track;
        this.samples = samples;
    }

    public Mp4Track getTrack() {
        return track;
    }

    public List<Mp4Sample> getSamples() {
        return samples;
    }
}
