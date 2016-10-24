package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.AmfObjectWrapper;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public interface RtmpSubscriber {
    void acceptVideo(ByteBuf readonly, int timestamp);
    void acceptAudio(ByteBuf readonly, int timestamp);
    void begin(AmfObjectWrapper metadata, ByteBuf videoro, ByteBuf audioro);
    void finish();
}
