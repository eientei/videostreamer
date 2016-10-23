package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Created by Alexander Tumin on 2016-10-20
 */
public class RtmpCodecHandler extends ChannelOutboundHandlerAdapter {

    private final RtmpContext context;

    public RtmpCodecHandler(RtmpContext context) {
        this.context = context;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof RtmpMessage) {
            RtmpMessage message = (RtmpMessage) msg;

            RtmpDisassembler disassembler = context.getDisassembler(message.getChunk());
            ByteBuf encoded = disassembler.disassemble(message);
            ctx.write(encoded, promise);
            message.getData().release();
        } else if (msg instanceof ByteBuf) {
            ctx.write(msg, promise);
        }
    }
}
