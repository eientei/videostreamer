package org.eientei.videostreamer.mp4;

import org.eientei.videostreamer.mp4.boxes.Mp4MdatBox;
import org.eientei.videostreamer.mp4.boxes.Mp4MoofBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class Mp4Frame {
    private Mp4MdatBox mdat;
    private List<SamplesPack> samples = new ArrayList<>();

    public void append(Mp4Track track) {
        if (track.isSamplesReady()) {
            List<Mp4Sample> newsamples = track.drainSamples();
            samples.add(new SamplesPack(track, newsamples));
        }
    }

    public Mp4MoofBox getMoof(Mp4RemuxerHandler context, List<Mp4Track> tracks, Mp4SubscriberContext subscriber) {
        return new Mp4MoofBox(context, tracks, this, subscriber);
    }

    public Mp4MdatBox getMdat(Mp4RemuxerHandler context, List<Mp4Track> tracks) {
        if (mdat == null) {
            mdat = new Mp4MdatBox(context, tracks, this);
        }
        return mdat;
    }

    public List<Mp4Sample> getSamples(Mp4Track track) {
        for (SamplesPack pack : samples) {
            if (pack.getTrack() == track) {
                return pack.getSamples();
            }
        }
        return null;
    }

    public int getTotalSize(Mp4Track track) {
        int size = 0;
        for (Mp4Sample sample : getSamples(track)) {
            size += sample.getCopydata().readableBytes();
        }
        return size;
    }

    public void release() {
        for (SamplesPack pack : samples) {
            for (Mp4Sample sample : pack.getSamples()) {
                sample.release();
            }
        }
    }

    public boolean isKeyframe(Mp4Track track) {
        for (Mp4Sample sample : getSamples(track)) {
            if (sample.isKeyframe()) {
                return true;
            }
        }
        return false;
    }

    public int getMinTimestamp(Mp4Track track) {
        int min = Integer.MAX_VALUE;
        for (Mp4Sample s : getSamples(track)) {
            min = Math.min(min, s.getTimestamp());
        }
        return min;
    }

    public int getMaxTimestamp(Mp4Track track) {
        int max = 0;
        for (Mp4Sample s : getSamples(track)) {
            max = Math.max(max, s.getTimestamp());
        }
        return max;
    }

    public int getMaxTimestamp(List<Mp4Track> tracks) {
        int max = 0;
        for (Mp4Track track : tracks) {
            max = Math.max(max, getMaxTimestamp(track));
        }
        return max;
    }

    public int getMinTimestamp(List<Mp4Track> tracks) {
        int min = Integer.MAX_VALUE;
        for (Mp4Track track : tracks) {
            min = Math.min(min, getMinTimestamp(track));
        }
        return min;
    }
}
