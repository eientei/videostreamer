package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-20
 */
public class RtmpDecoderHandler extends ReplayingDecoder<RtmpStage> {
    private final Logger log = LoggerFactory.getLogger(RtmpDecoderHandler.class);
    private final RtmpContext context;

    public RtmpDecoderHandler(RtmpContext context) {
        super(RtmpStage.STAGE0);
        this.context = context;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case STAGE0:
                if (!RtmpHandshake.stage0(in)) {
                    ctx.close();
                }
                checkpoint(RtmpStage.STAGE1);
                break;
            case STAGE1:
                if (!RtmpHandshake.stage1(context, in)) {
                    ctx.close();
                }
                checkpoint(RtmpStage.STAGE2);
                break;
            case STAGE2:
                if (!RtmpHandshake.stage2(in)) {
                    ctx.close();
                }
                checkpoint(RtmpStage.HEADER);
                break;
            case HEADER:
                int fst = in.readUnsignedByte();
                RtmpHeaderSize size = RtmpHeaderSize.dispatch((fst >> 6) & 0x03);
                int chunkid = readId(fst & 0x3f, in);
                RtmpAssembler assembler = context.getAssembler(chunkid);
                assembler.parseHeader(size, in);
                context.setLastChunkin(chunkid);
                checkpoint(RtmpStage.BODY);
                break;
            case BODY:
                assembler = context.getAssembler(context.getLastChunkin());
                assembler.update(in);
                checkpoint(RtmpStage.HEADER);
                break;
        }
    }

    private int readId(int chunkid, ByteBuf in) {
        switch (chunkid) {
            case 0:
                return 64 + in.readByte();
            case 1:
                return 64 + in.readShortLE();
        }
        return chunkid;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        context.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
    }
}
