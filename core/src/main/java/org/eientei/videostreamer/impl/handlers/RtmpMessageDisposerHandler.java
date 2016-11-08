package org.eientei.videostreamer.impl.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eientei.videostreamer.impl.core.Message;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class RtmpMessageDisposerHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        // do nothing but dispose incoming msgs (done by superclass)
    }
}
