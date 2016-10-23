package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpContext;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpWinackMessage extends RtmpMessage {
    public RtmpWinackMessage(int chunk, int stream, int time, ByteBuf data) {
        super(RtmpMessageType.WINACK, chunk, stream, time, data);
    }

    public RtmpWinackMessage(int chunk, int stream, int time, RtmpContext context, int winsize) {
        super(RtmpMessageType.WINACK, chunk, stream, time, context.ALLOC.alloc(4));
        getData().writeInt(winsize);
    }

    public int getWinsize() {
        return getData().getInt(0);
    }
}
