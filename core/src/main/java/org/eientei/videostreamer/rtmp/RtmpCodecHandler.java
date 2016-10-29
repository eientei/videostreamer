package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class RtmpCodecHandler extends MessageToByteEncoder<RtmpMessage> {
    private final Map<Integer, RtmpDisassembler> disassembly = new ConcurrentHashMap<>();
    private int chunkout = 128;

    @Override
    protected void encode(ChannelHandlerContext ctx, RtmpMessage msg, ByteBuf out) throws Exception {
        RtmpDisassembler disassembler = getDisassembler(msg.getChunk());
        disassembler.disassemble(msg, out);
    }

    private RtmpDisassembler getDisassembler(int chunk) {
        if (!disassembly.containsKey(chunk)) {
            disassembly.put(chunk, new RtmpDisassembler(chunk, this));
        }
        return disassembly.get(chunk);
    }

    public int getChunkout() {
        return chunkout;
    }

    public void setChunkout(int chunkout) {
        this.chunkout = chunkout;
    }
}
