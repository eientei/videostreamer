package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class RtmpMessageDecoderHandler extends ReplayingDecoder {
    public static class DecodeStreamContext {
        public DecodeStreamContext() {
        }

        private int time;
        private int length;
        private Message.Type type;
        private int stream;

        private int diff;
        private ByteBuf data;
    }


    private final Map<Integer, DecodeStreamContext> incontexts = new HashMap<>();
    private byte[] inbuf = new byte[128];
    private int ackwindow = 5000000;
    private int bytes = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int fst = in.readUnsignedByte();
        Header.Type type = Header.Type.values()[(fst >> 6) & 0x03];
        int chunk = parseChunkId(fst & 0x3f, in);

        DecodeStreamContext context = incontexts.get(chunk);
        if (context == null) {
            incontexts.put(chunk, context = new DecodeStreamContext());
            context.data = ctx.alloc().buffer();
        }

        Header header = parseHeader(context, chunk, type, in);
        int prebody = context.data.readableBytes();
        parseBody(context, in);
        checkpoint();

        if (context.length == context.data.readableBytes()) {
            complete(ctx, context, header, out);
        }

        if (type != Header.Type.NONE || prebody == 0) {
            context.diff = header.getTime() - context.time;
            context.stream = header.getStream();
            context.type = header.getType();
            context.time = header.getTime();
        }
    }

    private void complete(ChannelHandlerContext ctx, DecodeStreamContext context, Header header, List<Object> out) {
        out.add(new Message(header, context.data));
        switch (header.getType()) {
            case SET_CHUNK_SIZE:
                inbuf = new byte[context.data.getInt(0)];
                break;
            case WINACK:
                ackwindow = context.data.getInt(0);
                break;
        }
        bytes += context.length;
        if (bytes >= ackwindow) {
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeInt(bytes);
            ctx.writeAndFlush(new Message(new Header(2, 0, Message.Type.ACK, 0), buf));
            buf.release();
            bytes = 0;
        }
        context.data.release();
        context.data = ctx.alloc().buffer();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        for (DecodeStreamContext context : incontexts.values()) {
            if (context.data != null) {
                context.data.release();
            }
        }
        super.channelUnregistered(ctx);
    }

    private void parseBody(DecodeStreamContext context, ByteBuf msg) {
        int remaining = context.length - context.data.readableBytes();
        if (remaining > inbuf.length) {
            remaining = inbuf.length;
        }
        msg.readBytes(inbuf, 0, remaining);
        context.data.writeBytes(inbuf, 0, remaining);
    }

    public Header parseHeader(DecodeStreamContext context, int chunk, Header.Type type, ByteBuf msg) {
        switch (type) {
            default:
            case FULL:
                return parseFullHeader(context, chunk, msg);
            case MEDIUM:
                return parseMediumHeader(context, chunk, msg);
            case SMALL:
                return parseSmallHeader(context, chunk, msg);
            case NONE:
                return parseNoneHeader(context, chunk, msg);
        }
    }

    public Header parseFullHeader(DecodeStreamContext context, int chunk, ByteBuf msg) {
        int time = msg.readUnsignedMedium();
        context.length = msg.readUnsignedMedium();
        int typeid = msg.readUnsignedByte();
        int stream = (int) msg.readUnsignedIntLE();
        if (time == 0xFFFFFF) {
            time = (int) msg.readUnsignedInt();
        }
        Message.Type type = null;
        for (Message.Type t: Message.Type.values()) {
            if (t.getValue() == typeid) {
                type = t;
                break;
            }
        }
        return new Header(chunk, time, type, stream);
    }

    public Header parseMediumHeader(DecodeStreamContext context, int chunk, ByteBuf msg) {
        int diff = msg.readUnsignedMedium();
        context.length = msg.readUnsignedMedium();
        int typeid = msg.readUnsignedByte();
        Message.Type type = null;
        for (Message.Type t : Message.Type.values()) {
            if (t.getValue() == typeid) {
                type = t;
                break;
            }
        }
        return new Header(chunk, context.time+diff, type, context.stream);
    }

    private Header parseSmallHeader(DecodeStreamContext context, int chunk, ByteBuf msg) {
        int diff = msg.readUnsignedMedium();
        return new Header(chunk, context.time+diff, context.type, context.stream);
    }

    private Header parseNoneHeader(DecodeStreamContext context, int chunk, ByteBuf msg) {
        return new Header(chunk, context.time+context.diff, context.type, context.stream);
    }

    private int parseChunkId(int fst, ByteBuf in) {
        switch (fst) {
            case 0:
                return 64 + in.readByte();
            case 1:
                return 64 + in.readShortLE();
        }
        return fst;
    }
}
