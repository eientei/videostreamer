package org.eientei.videostreamer.impl.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.StreamContext;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class RtmpMessageInboundBroadcastHandler extends SimpleChannelInboundHandler<Message> {
    private final StreamContext context;

    public RtmpMessageInboundBroadcastHandler(StreamContext context) {
        this.context = context;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        msg.retain();
        context.publish(msg);
    }
}
