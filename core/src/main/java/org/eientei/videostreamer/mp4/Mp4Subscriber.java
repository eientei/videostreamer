package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public interface Mp4Subscriber {
    void init(String codecs);
    void accept(Mp4Box... boxes);
    void addTick(int trackid, int amount);
    int getTick(int trackid);
    void close();
}
