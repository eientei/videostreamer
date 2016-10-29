package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.eientei.videostreamer.rtmp.message.RtmpAckMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class RtmpDecoderHandler extends ReplayingDecoder<RtmpStage> {
    private int lastChunkdId;
    private int chunkin = 128;
    private int readBytes = 0;
    private int ackwindow = 5000000;
    private final Map<Integer, RtmpAssembler> assembly = new ConcurrentHashMap<>();

    public RtmpDecoderHandler() {
        super(RtmpStage.HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER:
                int fst = in.readUnsignedByte();
                RtmpHeaderSize size = RtmpHeaderSize.dispatch((fst >> 6) & 0x03);
                int chunkid = readId(fst & 0x3f, in);
                RtmpAssembler assembler = getAssembler(ctx, chunkid);
                assembler.parseHeader(size, in);
                lastChunkdId = chunkid;
                checkpoint(RtmpStage.BODY);
                break;
            case BODY:
                assembler = getAssembler(ctx, lastChunkdId);
                assembler.update(in, out);
                checkpoint(RtmpStage.HEADER);
                break;
        }

        if (readBytes >= ackwindow) {
            ctx.writeAndFlush(new RtmpAckMessage(2, 0, 0, ctx.alloc().buffer(), readBytes));
            readBytes = 0;
        }
    }

    private RtmpAssembler getAssembler(ChannelHandlerContext ctx, int chunk) {
        if (!assembly.containsKey(chunk)) {
            assembly.put(chunk, new RtmpAssembler(ctx, this, chunk));
        }
        return assembly.get(chunk);
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

    public int getChunkin() {
        return chunkin;
    }

    public void setChunkin(int chunkin) {
        this.chunkin = chunkin;
    }


    public int getAckwindow() {
        return ackwindow;
    }

    public void setAckwindow(int ackwindow) {
        this.ackwindow = ackwindow;
    }

    public void updateRead(int remain) {
        readBytes += remain;
    }
}
