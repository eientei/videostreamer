package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public interface Mp4PublishNotifier {
    void publish(String name, String codecs);
    void unpublish(String name);
    void subscribers(int size);
}
