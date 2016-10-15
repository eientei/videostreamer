package org.eientei.videostreamer.mp4;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4TrackFrame {
    public List<Mp4VideoSample> samples = new ArrayList<>();
    public int tfhdFlags;
    public int tfhdSampleFlags;
    public Mp4Track track;
    public int basetime;
    public int trunFlags;
    public int sizptr;

    public Mp4TrackFrame(Mp4VideoTrack track, List<Mp4VideoSample> samples) {
        this.track = track;
        this.samples.addAll(samples);
        basetime = (int) track.ticks;
        trunFlags = 0x01 | 0x04 | 0x100 | 0x0200;
        tfhdFlags = 0x20 | 0x20000;
        tfhdSampleFlags = 0x01010000;
    }

    public void dispose() {
        for (Mp4VideoSample sample : samples) {
            sample.dispose();
        }
        samples.clear();
    }
}
