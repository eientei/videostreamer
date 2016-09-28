package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.eientei.videostreamer.rtmp.message.RtmpSetChunkSizeMessage;

/**
 * Created by Alexander Tumin on 2016-09-26
 */
public class RtmpMessageCodec extends MessageToByteEncoder<RtmpMessage> {
    private byte[] chunk = new byte[128];
    private RtmpMessageChunker[] templates = new RtmpMessageChunker[RtmpServer.RTMP_CHUNK_MAX];
    private boolean init;

    @Override
    protected void encode(ChannelHandlerContext ctx, RtmpMessage msg, ByteBuf out) throws Exception {
        RtmpUnchunkedMessage unchunked = new RtmpUnchunkedMessage(msg.getHeader());
        msg.serialize(unchunked.getData());
        unchunked.getHeader().setLength(unchunked.getData().readableBytes());

        for (int i  = 0; i < unchunked.getHeader().getLength();) {
            boolean first = i == 0;
            RtmpMessageChunker template = getTemplate(unchunked.getHeader().getChunkid());
            if (isNotFull(template, unchunked.getHeader())) {
                if (isNotMedium(template, unchunked.getHeader())) {
                    if (isNotShort(template, unchunked.getHeader())) {
                        writeBasicHeader(first, out, RtmpHeader.Size.NONE, template, unchunked.getHeader());
                    } else {
                        writeShortHeader(first, out, template, unchunked.getHeader());
                    }
                } else {
                    writeMediumHeader(first, out, template, unchunked.getHeader());
                }
            } else {
                writeFullHeader(first, out, template, unchunked.getHeader());
            }

            int remain = unchunked.getData().readableBytes();
            if (remain > chunk.length) {
                remain = chunk.length;
            }
            unchunked.getData().readBytes(chunk, 0, remain);
            out.writeBytes(chunk, 0, remain);
            i += remain;
        }

        lateHandle(msg);
    }

    private void lateHandle(RtmpMessage msg) {
        if (msg instanceof RtmpSetChunkSizeMessage) {
            chunk = new byte[(int) ((RtmpSetChunkSizeMessage) msg).getChunkSize()];
        }
    }

    private void writeFullHeader(boolean first, ByteBuf out, RtmpMessageChunker template, RtmpHeader header) {
        template.setTimeDiff(0);
        writeBasicHeader(first, out, RtmpHeader.Size.FULL, template, header);
        template.getHeader().setTimestamp(header.getTimestamp());
        template.getHeader().setLength(header.getLength());
        template.getHeader().setType(header.getType());
        template.getHeader().setStreamid(header.getStreamid());
        out.writeMedium((int) (header.getTimestamp() >= 0xFFFFFF ? 0xFFFFFF : header.getTimestamp()));
        out.writeMedium(header.getLength());
        out.writeByte(header.getType().getValue());
        out.writeInt((int) header.getStreamid());
        if (header.getTimestamp() >= 0xFFFFFF) {
            out.writeInt((int) header.getTimestamp());
        }
    }

    private void writeMediumHeader(boolean first, ByteBuf out, RtmpMessageChunker template, RtmpHeader header) {
        template.setTimeDiff(header.getTimestamp() - template.getHeader().getTimestamp());
        writeBasicHeader(first, out, RtmpHeader.Size.MEDIUM, template, header);
        template.getHeader().setLength(header.getLength());
        template.getHeader().setType(header.getType());
        out.writeMedium((int) template.getTimeDiff());
        out.writeMedium(header.getLength());
        out.writeByte(header.getType().getValue());
    }

    private void writeShortHeader(boolean first, ByteBuf out, RtmpMessageChunker template, RtmpHeader header) {
        template.setTimeDiff(header.getTimestamp() - template.getHeader().getTimestamp());
        writeBasicHeader(first, out, RtmpHeader.Size.SHORT, template, header);
        out.writeMedium((int) template.getTimeDiff());
    }

    private void writeBasicHeader(boolean first, ByteBuf out, RtmpHeader.Size size, RtmpMessageChunker template, RtmpHeader header) {
        if (first) {
            template.getHeader().setTimestamp(template.getHeader().getTimestamp() + template.getTimeDiff());
        }
        int chunkid = header.getChunkid();

        int fst = (size.getValue() << 6);

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

    private boolean isNotShort(RtmpMessageChunker template, RtmpHeader header) {
        return template.getHeader().getTimestamp() + template.getTimeDiff() == header.getTimestamp();
    }

    private boolean isNotMedium(RtmpMessageChunker template, RtmpHeader header) {
        return template.getHeader().getType() == header.getType() &&
                template.getHeader().getLength() == header.getLength();
    }

    private boolean isNotFull(RtmpMessageChunker template, RtmpHeader header) {
        return template.getHeader().getStreamid() == header.getStreamid() && header.getTimestamp() < 0xFFFFFF && !init;
    }

    private RtmpMessageChunker getTemplate(int chunkid) {
        RtmpMessageChunker template = templates[chunkid];
        init = false;
        if (template == null) {
            template = new RtmpMessageChunker(chunkid);
            templates[chunkid] = template;
            init = true;
        }
        return template;
    }
}
