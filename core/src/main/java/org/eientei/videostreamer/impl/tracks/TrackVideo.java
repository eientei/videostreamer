package org.eientei.videostreamer.impl.tracks;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public interface TrackVideo {
    int getWidth();
    int getHeight();
    ByteBuf getInit();
}
