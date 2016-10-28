package org.eientei.videostreamer.mp4;

import org.eientei.videostreamer.ws.CommType;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public interface Mp4Subscriber {
    void begin(String codecs);
    void accept(CommType type, int begin, int end, Mp4Box... boxes);
    void finish();
    void count(int subscribers);
}
