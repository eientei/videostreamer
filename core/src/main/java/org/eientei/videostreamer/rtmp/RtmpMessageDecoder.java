package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.eientei.videostreamer.rtmp.message.RtmpAckMessage;
import org.eientei.videostreamer.rtmp.message.RtmpSetChunkSizeMessage;
import org.eientei.videostreamer.rtmp.message.RtmpWinackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpMessageDecoder extends ReplayingDecoder<RtmpMessageDecoder.State> {
    private Logger log = LoggerFactory.getLogger(RtmpMessageDecoder.class);

    public enum State {
        HEADER,
        CHUNK
    }

    private byte[] chunk = new byte[128];
    private long ackwindow = 0;
    private long ackbytes = 0;
    private RtmpMessageAssembler[] templates = new RtmpMessageAssembler[RtmpServer.RTMP_CHUNK_MAX];
    private RtmpMessageAssembler template;

    public RtmpMessageDecoder() {
        super(State.HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER:
                readBasic(in);
                checkpoint(State.CHUNK);
                break;
            case CHUNK:
                readChunk(in);
                checkFully(ctx, out);
                checkpoint(State.HEADER);
                break;
        }
    }

    private void checkFully(ChannelHandlerContext ctx, List<Object> out) {
        if (currentSize() == template.getCurrent().getHeader().getLength()) {
            ackbytes += currentSize();
            if (ackwindow != 0 && ackbytes > ackwindow) {
                ctx.writeAndFlush(new RtmpAckMessage(ackbytes));
                ackbytes = 0;
            }
            RtmpMessage message = template.getCurrent().getHeader().getType().getParser().parse(template.getCurrent());
            message.getHeader().setFrom(template.getCurrent().getHeader());
            earlyMessageHandle(message);
            out.add(message);
            template.reset();
        }
        template = null;
    }

    private int currentSize() {
        return template.getCurrent().getData().writerIndex();
    }

    private void earlyMessageHandle(RtmpMessage message) {
        if (message instanceof RtmpSetChunkSizeMessage) {
            int size = (int) ((RtmpSetChunkSizeMessage) message).getChunkSize();
            chunk = new byte[size];
        } else if (message instanceof RtmpWinackMessage) {
            ackwindow = ((RtmpWinackMessage) message).getSize();
        }
    }

    private void readChunk(ByteBuf in) {
        int currentSize = currentSize();
        int remain = template.getCurrent().getHeader().getLength() - currentSize;
        if (remain > chunk.length) {
            remain = chunk.length;
        }

        in.readBytes(chunk, 0, remain);
        template.getCurrent().getData().writeBytes(chunk, 0, remain);
    }

    private void readBasic(ByteBuf in) {
        int fst = in.readUnsignedByte();
        int fmt = (fst>>6) & 0x03;

        RtmpHeader.Size size = RtmpHeader.Size.parseValue(fmt);
        int chunkid = readId(fst & 0x3f, in);
        template = getTemplate(chunkid);
        template.nextChunk(size, in);
    }

    private RtmpMessageAssembler getTemplate(int chunkid) {
        if (templates[chunkid] == null) {
            templates[chunkid] = new RtmpMessageAssembler(chunkid);
        }
        return templates[chunkid];
    }

    private int readId(int chunkid, ByteBuf in) {
        switch (chunkid) {
            case 0:
                return 64 +  in.readUnsignedByte();
            case 1:
                return 64 + in.readUnsignedShortLE();
        }
        return chunkid;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(RtmpServer.RTMP_CONNECTION_CONTEXT).get().close();
    }
}
