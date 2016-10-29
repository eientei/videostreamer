package org.eientei.videostreamer.mp4;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class Mp4SubscriberContext {
    private final Map<Mp4Track, Integer> tracktimes = new HashMap<>();
    private int begin;

    public Map<Mp4Track, Integer> getTracktimes() {
        return tracktimes;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }
}
