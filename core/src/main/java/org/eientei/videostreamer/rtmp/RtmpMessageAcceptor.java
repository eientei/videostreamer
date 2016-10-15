package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public interface RtmpMessageAcceptor {
    void accept(RtmpMessage message);
}