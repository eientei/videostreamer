package org.eientei.videostreamer.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eientei.videostreamer.server.ServerContext;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class WebsocketMessageHandler extends SimpleChannelInboundHandler<WebsocketTextQuery> {
    private final ServerContext globalContext;
    private String streamname;

    public WebsocketMessageHandler(ServerContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, WebsocketTextQuery msg) throws Exception {
        switch(msg.getAction()) {
            case "play":
                streamname = msg.getParams();
                globalContext.subscribeMuxer(streamname, ctx.channel());
                ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        globalContext.unsubscribeMuxer(streamname, ctx.channel());
                    }
                });
                break;
            case "stop":
                globalContext.unsubscribeMuxer(streamname, ctx.channel());
                break;
        }
    }
}
