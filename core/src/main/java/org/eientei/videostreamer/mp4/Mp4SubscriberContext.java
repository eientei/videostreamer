package org.eientei.videostreamer.mp4;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-26
 */
public class Mp4SubscriberContext {
    private final Map<Mp4Track, Double> tracktimes = new HashMap<>();
    private double begin;

    public Map<Mp4Track, Double> getTracktimes() {
        return tracktimes;
    }


    public void clear() {
        tracktimes.clear();
    }

    public void setBegin(double begin) {
        this.begin = begin;
    }

    public double getBegin() {
        return begin;
    }
}
