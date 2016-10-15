package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;
import org.eientei.videostreamer.rtmp.server.RtmpMessageCodec;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpDisassembler {
    private final RtmpMessageCodec codec;

    private RtmpMessageType type;
    private long streamid;
    private long time;
    private int length;

    private long timediff;
    private boolean init = true;


    public RtmpDisassembler(RtmpMessageCodec codec) {
        this.codec = codec;
    }

    public void disassemble(RtmpMessage msg, ByteBuf out) {
        boolean compressable = !init && msg instanceof RtmpAudioMessage || msg instanceof RtmpVideoMessage;
        ByteBuf in = msg.getData().slice();
        if (compressable && isNotFull(msg)) {
            if (isNotMedium(msg)) {
                if (isNotShort(msg)) {
                    writeBasicHeader(msg.getHeader(), RtmpHeaderType.NONE, out);
                    time = time+timediff;
                } else {
                    writeBasicHeader(msg.getHeader(), RtmpHeaderType.SHORT, out);
                    writeShortHeader(msg, out);
                    time = time+timediff;
                }
            } else {
                writeBasicHeader(msg.getHeader(), RtmpHeaderType.MEDIUM, out);
                writeMediumHeader(msg, out);
                time = time+timediff;
            }
        } else {
            writeBasicHeader(msg.getHeader(), RtmpHeaderType.FULL, out);
            writeFullHeader(msg, out);
        }

        writeNext(in, out);

        while (in.isReadable()) {
            writeBasicHeader(msg.getHeader(), RtmpHeaderType.NONE, out);
            writeNext(in, out);
        }
    }

    private void writeFullHeader(RtmpMessage msg, ByteBuf out) {
        init = false;
        timediff = msg.getHeader().getTime() - time;
        time = msg.getHeader().getTime();
        length = msg.getData().readableBytes();
        type = msg.getHeader().getType();
        streamid = msg.getHeader().getStreamid();

        out.writeMedium((int) (time >= 0xFFFFFF ? 0xFFFFFF : time));
        out.writeMedium(length);
        out.writeByte(type.getValue());
        out.writeIntLE((int) streamid);
        if (time >= 0xFFFFFF) {
            out.writeInt((int) time);
        }
    }

    private void writeMediumHeader(RtmpMessage msg, ByteBuf out) {
        timediff = msg.getHeader().getTime() - time;
        length = msg.getData().readableBytes();
        type = msg.getHeader().getType();

        out.writeMedium((int) timediff);
        out.writeMedium(length);
        out.writeByte(type.getValue());
    }

    private void writeShortHeader(RtmpMessage msg, ByteBuf out) {
        timediff = msg.getHeader().getTime() - time;
        out.writeMedium((int) timediff);
    }

    private void writeBasicHeader(RtmpHeader header, RtmpHeaderType htype, ByteBuf out) {
        int fst = (htype.getValue() << 6);

        int chunkid = header.getChunkid();
        if (chunkid >= 320) {
            out.writeByte(fst | 1);
            out.writeShortLE(chunkid);
        } else if (chunkid >= 64) {
            out.writeByte(fst);
            out.writeByte(chunkid);
        } else {
            out.writeByte(fst | chunkid);
        }
    }

    private void writeNext(ByteBuf in, ByteBuf out) {
        int remain = in.readableBytes();
        if (remain > codec.getChunksize()) {
            remain = codec.getChunksize();
        }
        out.ensureWritable(remain);
        in.readBytes(out, remain);
    }

    private boolean isNotShort(RtmpMessage msg) {
        RtmpHeader h = msg.getHeader();
        return time + timediff == h.getTime();
    }

    private boolean isNotMedium(RtmpMessage msg) {
        RtmpHeader h = msg.getHeader();
        return type == h.getType() && length == msg.getData().readableBytes();
    }

    private boolean isNotFull(RtmpMessage msg) {
        RtmpHeader h = msg.getHeader();
        return streamid == h.getStreamid() && h.getTime() < 0xFFFFFF;
    }
}
