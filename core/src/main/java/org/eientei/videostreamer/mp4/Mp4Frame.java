package org.eientei.videostreamer.mp4;

import org.eientei.videostreamer.mp4.boxes.Mp4MdatBox;
import org.eientei.videostreamer.mp4.boxes.Mp4MoofBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Frame {
    private Mp4MdatBox mdat;
    private Map<Mp4Track, List<Mp4Sample>> samples = new HashMap<>();
    private boolean keyframe;

    public void append(Mp4Track track) {
        if (track.isSamplesReady()) {
            List<Mp4Sample> newsamples = track.drainSamples();
            if (samples.isEmpty()) {
                for (Mp4Sample sample : newsamples) {
                    keyframe = keyframe || sample.isKeyframe();
                }
            }
            samples.put(track, newsamples);
        }
    }

    public Mp4MoofBox getMoof(Mp4Context context, List<Mp4Track> tracks, Mp4SubscriberContext subscriber) {
        return new Mp4MoofBox(context, tracks, this, subscriber);
    }

    public Mp4MdatBox getMdat(Mp4Context context, List<Mp4Track> tracks) {
        if (mdat == null) {
            mdat = new Mp4MdatBox(context, tracks, this);
        }
        return mdat;
    }

    public List<Mp4Sample> getSamples(Mp4Track track) {
        return samples.get(track);
    }

    public int getTotalSize(Mp4Track track) {
        int size = 0;
        for (Mp4Sample sample : getSamples(track)) {
            size += sample.getData().readableBytes();
        }
        return size;
    }

    public void release() {
        for (List<Mp4Sample> list : samples.values()) {
            for (Mp4Sample sample : list) {
                sample.release();
            }
        }
    }

    public boolean isKeyframe() {
        return keyframe;
    }

    public double getMinTimestamp(Mp4Track track) {
        double min = Integer.MAX_VALUE;
        for (Mp4Sample s : samples.get(track)) {
            min = Math.min(min, s.getTimestamp());
        }
        return min;
    }

    public double getMaxTimestamp(Mp4Track track) {
        double max = 0;
        for (Mp4Sample s : samples.get(track)) {
            max = Math.max(max, s.getTimestamp());
        }
        return max;
    }

    public double getMaxTimestamp(List<Mp4Track> tracks) {
        double max = 0;
        for (Mp4Track track : tracks) {
            max = Math.max(max, getMaxTimestamp(track));
        }
        return max;
    }

    public double getMinTimestamp(List<Mp4Track> tracks) {
        double min = Integer.MAX_VALUE;
        for (Mp4Track track : tracks) {
            min = Math.min(min, getMinTimestamp(track));
        }
        return min;
    }
}
