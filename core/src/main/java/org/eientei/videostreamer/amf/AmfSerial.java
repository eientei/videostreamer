package org.eientei.videostreamer.amf;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-09-26
 */
public interface AmfSerial<T> {
    T deserialize(ByteBuf data);
    void serialize(ByteBuf data, Object obj);
}
