package org.eientei.videostreamer.ws;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class WebsocketCodecHandler extends ChannelOutboundHandlerAdapter {
    private final WebSocketSession session;

    public WebsocketCodecHandler(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        session.sendMessage(new BinaryMessage(buf.array(), buf.arrayOffset()+buf.readerIndex(), buf.readableBytes(), true));
        buf.release();
        promise.setSuccess();
    }
}
