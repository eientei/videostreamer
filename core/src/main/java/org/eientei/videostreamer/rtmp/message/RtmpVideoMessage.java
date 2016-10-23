package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpVideoMessage extends RtmpMessage {
    public RtmpVideoMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.VIDEO, chunk, stream, time, data);
    }
}
