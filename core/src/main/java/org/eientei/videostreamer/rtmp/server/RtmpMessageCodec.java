package org.eientei.videostreamer.rtmp.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.eientei.videostreamer.rtmp.RtmpDisassembler;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.message.RtmpSetChunkSizeMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpMessageCodec extends MessageToByteEncoder<RtmpMessage> {
    private Map<Integer, RtmpDisassembler> disassemblers = new HashMap<>();
    private int chunksize = 128;

    @Override
    protected synchronized void encode(ChannelHandlerContext ctx, RtmpMessage msg, ByteBuf out) throws Exception {
        getDisassembler(msg.getHeader().getChunkid()).disassemble(msg, out);
        if (msg instanceof RtmpSetChunkSizeMessage) {
            chunksize = ((RtmpSetChunkSizeMessage) msg).getChunkSize();
        }
        msg.release();
    }

    public int getChunksize() {
        return chunksize;
    }

    public void setChunksize(int chunksize) {
        this.chunksize = chunksize;
    }

    private RtmpDisassembler getDisassembler(int chunkid) {
        if (!disassemblers.containsKey(chunkid)) {
            disassemblers.put(chunkid, new RtmpDisassembler(this));
        }
        return disassemblers.get(chunkid);
    }
}
