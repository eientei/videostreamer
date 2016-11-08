package org.eientei.videostreamer.impl.core;

/**
 * Created by Alexander Tumin on 2016-11-08
 */
public interface StreamEventListener {
    void play(String name);
    void stop(String name);
}
