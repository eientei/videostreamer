package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpMetaMessage extends RtmpMessage {
    public RtmpMetaMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.AMF0_META, chunk, stream, time, data);
    }

    public RtmpMetaMessage(int chunk, int stream, int time, ByteBuf buf, Object... data) {
        super(RtmpMessageType.AMF0_META, chunk, stream, time, buf);
        for (Object obj : data) {
            Amf.serialize(getData(), obj);
        }
    }


}
