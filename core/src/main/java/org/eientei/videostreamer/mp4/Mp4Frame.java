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
    private Map<Integer, List<Mp4Sample>> samples = new HashMap<>();
    private boolean keyframe;

    public void append(Mp4Track track) {
        List<Mp4Sample> newsamples = track.drainSamples();
        if (samples.isEmpty()) {
            for (Mp4Sample sample : newsamples) {
                keyframe = keyframe || sample.isKeyframe();
            }
        }
        samples.put(track.id(), newsamples);
    }

    public Mp4MoofBox getMoof(Mp4Context context, Mp4Subscriber subscriber) {
        return new Mp4MoofBox(context, this, subscriber);
    }

    public Mp4MdatBox getMdat(Mp4Context context) {
        if (mdat == null) {
            mdat = new Mp4MdatBox(context, this);
        }
        return mdat;
    }

    public List<Mp4Sample> getSamples(int id) {
        return samples.get(id);
    }

    public int getTotalSize(int id) {
        int size = 0;
        for (Mp4Sample sample : getSamples(id)) {
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
}
