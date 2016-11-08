package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class RtmpMessageEncoderHandler extends MessageToByteEncoder<Message> {
    public static class EncodeStreamContext {
        public EncodeStreamContext() {
        }

        private int time;
        private int length;
        private Message.Type type;
        private int stream;

        private int diff;
    }

    private final Map<Integer, EncodeStreamContext> outcontexts = new HashMap<>();
    private int outchunk = 128;

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        boolean wasMessage = true;
        EncodeStreamContext context = outcontexts.get(msg.getHeader().getChunk());
        if (context == null) {
            wasMessage = false;
            outcontexts.put(msg.getHeader().getChunk(), context = new EncodeStreamContext());
        }

        boolean compressable = wasMessage && (msg.getHeader().getType() == Message.Type.AUDIO || msg.getHeader().getType() == Message.Type.VIDEO);
        if (compressable && notFull(context, msg)) {
            if (notMedium(context, msg)) {
                if (notShort(context, msg)) {
                    makeBasicHeader(out, msg, Header.Type.NONE);
                    context.time = context.time + context.diff;
                } else {
                    makeBasicHeader(out, msg, Header.Type.SMALL);
                    makeSmallHeader(context, out, msg);
                }
            } else {
                makeBasicHeader(out, msg, Header.Type.MEDIUM);
                makeMediumHeader(context, out, msg);
            }
        } else {
            makeBasicHeader(out, msg, Header.Type.FULL);
            makeFullHeader(context, out, msg);
        }

        ByteBuf data = msg.getData();
        makeChunk(out, data, outchunk);
        while (data.isReadable()) {
            makeBasicHeader(out, msg, Header.Type.NONE);
            makeChunk(out, data, outchunk);
        }

        switch (msg.getHeader().getType()) {
            case SET_CHUNK_SIZE:
                outchunk = msg.getData().getInt(0);
                break;
        }
        //msg.release();
    }

    private void makeChunk(ByteBuf out, ByteBuf data, int chunk) {
        int remain = data.readableBytes();
        if (remain > chunk) {
            remain = chunk;
        }
        out.ensureWritable(remain);
        data.readBytes(out, remain);
    }

    private void makeFullHeader(EncodeStreamContext context, ByteBuf out, Message msg) {
        context.diff = msg.getHeader().getTime() - context.time;
        context.time = msg.getHeader().getTime();
        context.length = msg.getData().readableBytes();
        context.type = msg.getHeader().getType();
        context.stream = msg.getHeader().getStream();

        out
                .writeMedium(context.time >= 0xFFFFFF ? 0xFFFFFF : context.time)
                .writeMedium(context.length)
                .writeByte(context.type.getValue())
                .writeIntLE(context.stream);

        if (context.time >= 0xFFFFFF) {
            out.writeInt(context.time);
        }

    }

    private void makeMediumHeader(EncodeStreamContext context, ByteBuf out, Message msg) {
        context.diff = msg.getHeader().getTime() - context.time;
        context.length = msg.getData().readableBytes();
        context.type = msg.getHeader().getType();
        context.time = context.time + context.diff;
        out.writeMedium(context.diff).writeMedium(context.length).writeByte(context.type.getValue());
    }

    private void makeSmallHeader(EncodeStreamContext context, ByteBuf out, Message msg) {
        context.diff = msg.getHeader().getTime() - context.time;
        context.time = context.time + context.diff;
        out.writeMedium(context.diff);
    }

    private void makeBasicHeader(ByteBuf out, Message msg, Header.Type type) {
        int fst = type.getValue() << 6;
        if (msg.getHeader().getChunk() >= 320) {
            out.writeByte(fst | 1);
            out.writeShortLE(msg.getHeader().getChunk() - 64);
        } else if (msg.getHeader().getChunk() >= 64) {
            out.writeByte(fst);
            out.writeByte(msg.getHeader().getChunk() - 64);
        } else {
            out.writeByte(fst | msg.getHeader().getChunk());
        }
    }

    private boolean notShort(EncodeStreamContext context, Message msg) {
        return context.time + context.diff == msg.getHeader().getTime();
    }

    private boolean notMedium(EncodeStreamContext context, Message msg) {
        return context.type == msg.getHeader().getType() && context.length == msg.getData().readableBytes();
    }

    private boolean notFull(EncodeStreamContext context, Message msg) {
        return context.stream == msg.getHeader().getStream() && msg.getHeader().getTime() < 0xFFFFFF;
    }


}
