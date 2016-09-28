package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public interface RtmpStreamClient {
    void accept(RtmpMessage message);
}
