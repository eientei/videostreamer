package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public interface RtmpPublishNotifier {
    void publish(RtmpStream stream);
    void unpublish(RtmpStream stream);
}
