package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class RtmpDisassembler {
    private final int chunk;
    private final RtmpCodecHandler codec;

    private boolean first = true;
    private RtmpMessageType lastType;
    private int lastStreamid;
    private int lastTime;
    private int lastLength;
    private int lastTimediff;

    public RtmpDisassembler(int chunk, RtmpCodecHandler codec) {
        this.chunk = chunk;
        this.codec = codec;
    }

    public void disassemble(RtmpMessage message, ByteBuf out) {
        ByteBuf data = message.getData();

        boolean compressable = !first && (message instanceof RtmpAudioMessage || message instanceof RtmpVideoMessage);
        if (compressable && notFull(message)) {
            if (notMedium(message)) {
                if (notShort(message)) {
                    makeBasicHeader(out, message, RtmpHeaderSize.NONE);
                    lastTime += lastTimediff;
                } else {
                    makeBasicHeader(out, message, RtmpHeaderSize.SHORT);
                    makeShortHeader(out, message);
                    lastTime += lastTimediff;
                }
            } else {
                makeBasicHeader(out, message, RtmpHeaderSize.MEDIUM);
                makeMediumHeader(out, message);
                lastTime += lastTimediff;
            }
        } else {
            makeBasicHeader(out, message, RtmpHeaderSize.FULL);
            makeFullHeader(out, message);
            lastTime = message.getTime();
        }

        makeChunk(out, data);
        while (data.isReadable()) {
            makeBasicHeader(out, message, RtmpHeaderSize.NONE);
            makeChunk(out, data);
        }
    }

    private void makeChunk(ByteBuf out, ByteBuf data) {
        int remain = data.readableBytes();
        if (remain > codec.getChunkout()) {
            remain = codec.getChunkout();
        }
        out.ensureWritable(remain);
        data.readBytes(out, remain);
    }

    private void makeFullHeader(ByteBuf out, RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        lastTime = message.getTime();
        lastLength = message.getData().readableBytes();
        lastType = message.getType();
        lastStreamid = message.getStream();

        out
                .writeMedium(lastTime >= 0xFFFFFF ? 0xFFFFFF : lastTime)
                .writeMedium(lastLength)
                .writeByte(lastType.getValue())
                .writeIntLE(lastStreamid);

        if (lastTime >= 0xFFFFFF) {
            out.writeInt(lastTime);
        }
    }

    private void makeMediumHeader(ByteBuf out, RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        lastLength = message.getData().readableBytes();
        lastType = message.getType();

        out.writeMedium(lastTimediff).writeMedium(lastLength).writeByte(lastType.getValue());
    }

    private void makeShortHeader(ByteBuf out, RtmpMessage message) {
        lastTimediff = message.getTime() - lastTime;
        out.writeMedium(lastTimediff);
    }

    private void makeBasicHeader(ByteBuf out, RtmpMessage message, RtmpHeaderSize size) {
        int fst = size.getValue() << 6;
        if (message.getChunk() >= 320) {
            out.writeByte(fst | 1);
            out.writeShortLE(message.getChunk());
        } else if (message.getChunk() >= 64) {
            out.writeByte(fst);
            out.writeByte(message.getChunk());
        } else {
            out.writeByte(fst | message.getChunk());
        }
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
