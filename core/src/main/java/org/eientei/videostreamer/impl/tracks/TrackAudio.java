package org.eientei.videostreamer.impl.tracks;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public interface TrackAudio {
    int getChannels();
    int getSampleSize();
    int getSampleRate();
    ByteBuf getInit();
}
