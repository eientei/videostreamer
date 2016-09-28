package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public interface RtmpMessageParser<T extends RtmpMessage> {
    T parse(RtmpUnchunkedMessage msg);
}
