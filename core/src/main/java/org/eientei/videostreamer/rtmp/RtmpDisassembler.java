package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpDisassembler {
    private final int chunk;
    private final RtmpContext context;

    private boolean first = true;

    private RtmpMessageType lastType;
    private int lastStreamid;
    private int lastTime;
    private int lastLength;
    private int lastTimediff;

    public RtmpDisassembler(int chunk, RtmpContext context) {
        this.chunk = chunk;
        this.context = context;
    }

    public ByteBuf disassemble(RtmpMessage message) {
        ByteBuf data = message.getData();

        boolean compressable = !first && (message instanceof RtmpAudioMessage || message instanceof RtmpVideoMessage);

        CompositeByteBuf compose = context.ALLOC.compose();

        if (compressable && notFull(message)) {
            if (notMedium(message)) {
                if (notShort(message)) {
                    compose.addComponent(true, makeBasicHeader(message, RtmpHeaderSize.NONE));
                    lastTime += lastTimediff;
                } else {
                    compose.addComponent(true, makeBasicHeader(message, RtmpHeaderSize.SHORT));
                    compose.addComponent(true, makeShortHeader(message));
                    lastTime += lastTimediff;
                }
            } else {
                compose.addComponent(true, makeBasicHeader(message, RtmpHeaderSize.MEDIUM));
                compose.addComponent(true, makeMediumHeader(message));
                lastTime += lastTimediff;
            }
        } else {
            compose.addComponent(true, makeBasicHeader(message, RtmpHeaderSize.FULL));
            compose.addComponent(true, makeFullHeader(message));
            lastTime = message.getTime();
        }

        compose.addComponent(true, makeChunk(data));
        while (data.isReadable()) {
            compose.addComponent(true, makeBasicHeader(message, RtmpHeaderSize.NONE));
            compose.addComponent(true, makeChunk(data));
        }
        return compose;
    }

    private ByteBuf makeChunk(ByteBuf data) {
        int remain = data.readableBytes();
        if (remain > context.getChunkout()) {
            remain = context.getChunkout();
        }
        int pos = data.readerIndex();
        ByteBuf slice = data.copy(pos, remain);
        data.skipBytes(remain);
        return slice;
    }

    private ByteBuf makeFullHeader(RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        lastTime = message.getTime();
        lastLength = message.getData().readableBytes();
        lastType = message.getType();
        lastStreamid = message.getStream();

        ByteBuf byteBuf = context.ALLOC.alloc(lastTime >= 0xFFFFFF ? 15 : 11)
                .writeMedium(lastTime >= 0xFFFFFF ? 0xFFFFFF : lastTime)
                .writeMedium(lastLength)
                .writeByte(lastType.getValue())
                .writeIntLE(lastStreamid);

        if (lastTime >= 0xFFFFFF) {
            byteBuf.writeInt(lastTime);
        }

        return byteBuf;
    }

    private ByteBuf makeMediumHeader(RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        lastLength = message.getData().readableBytes();
        lastType = message.getType();

        return context.ALLOC.alloc(7).writeMedium(lastTimediff).writeMedium(lastLength).writeByte(lastType.getValue());
    }

    private ByteBuf makeShortHeader(RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        return context.ALLOC.alloc(3).writeMedium(lastTimediff);
    }

    private ByteBuf makeBasicHeader(RtmpMessage message, RtmpHeaderSize size) {
        int fst = size.getValue() << 6;
        ByteBuf alloc = context.ALLOC.alloc(getSize(message.getChunk()));
        if (message.getChunk() >= 320) {
            alloc.writeByte(fst | 1);
            alloc.writeShortLE(message.getChunk());
        } else if (message.getChunk() >= 64) {
            alloc.writeByte(fst);
            alloc.writeByte(message.getChunk());
        } else {
            alloc.writeByte(fst | message.getChunk());
        }
        return alloc;
    }

    private int getSize(int chunkid) {
        if (chunkid >= 320) {
            return 3;
        } else if (chunkid > 64) {
            return 2;
        } else {
            return 1;
        }
    }

    private boolean notShort(RtmpMessage message) {
        return lastTime + lastTimediff == message.getTime();
    }

    private boolean notMedium(RtmpMessage message) {
        return lastType == message.getType() && lastLength == message.getData().readableBytes();
    }

    private boolean notFull(RtmpMessage message) {
        return lastStreamid == message.getStream() && message.getTime() < 0xFFFFFF;
    }
}
