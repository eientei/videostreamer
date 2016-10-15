package org.eientei.videostreamer.rtmp.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.eientei.videostreamer.rtmp.RtmpAssembler;
import org.eientei.videostreamer.rtmp.RtmpHeaderType;
import org.eientei.videostreamer.rtmp.message.RtmpAckMessage;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpMessageDecoder extends ReplayingDecoder {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(RtmpMessageDecoder.class);
    private final Map<Integer, RtmpAssembler> assemblers = new HashMap<>();

    private int chunksize = 128;
    private int ackwindow = 5000000;
    private int readcount = 0;

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        stop(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
        stop(ctx);
    }

    private void stop(ChannelHandlerContext ctx) throws Exception {
        for (RtmpAssembler assembler : assemblers.values()) {
            assembler.release();
        }
        assemblers.clear();
        ctx.channel().attr(RtmpServer.CLIENT_CONTEXT).get().cleanup();
        super.channelUnregistered(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int fst = in.readUnsignedByte();

        RtmpHeaderType htype = RtmpHeaderType.dispatch((fst>>6) & 0x03);
        int chunkid = readId(fst & 0x3f, in);

        getAssembler(chunkid).update(htype, in, out);
        if (readcount >= ackwindow) {
            ctx.writeAndFlush(new RtmpAckMessage(2, 0, 0, readcount));
            readcount = 0;
        }

        checkpoint();
    }

    public int getChunksize() {
        return chunksize;
    }

    public void setChunksize(int chunksize) {
        this.chunksize = chunksize;
    }

    public int getAckwindow() {
        return ackwindow;
    }

    public void setAckwindow(int ackwindow) {
        this.ackwindow = ackwindow;
    }

    public int getReadcount() {
        return readcount;
    }

    public void setReadcount(int readcount) {
        this.readcount = readcount;
    }

    public void addReadcount(int readcount) {
        this.readcount += readcount;
    }

    private RtmpAssembler getAssembler(int chunkid) {
        if (!assemblers.containsKey(chunkid)) {
            assemblers.put(chunkid, new RtmpAssembler(this, chunkid));
        }
        return assemblers.get(chunkid);
    }

    private int readId(int chunkid, ByteBuf in) {
        switch (chunkid) {
            case 0:
                return 64 + in.readUnsignedByte();
            case 1:
                return 64 + in.readUnsignedShortLE();
        }
        return chunkid;
    }
}
