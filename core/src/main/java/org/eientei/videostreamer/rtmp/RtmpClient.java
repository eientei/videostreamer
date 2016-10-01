package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public interface RtmpClient {
    void accept(RtmpMessage message);
    void init(String streamName);
}
