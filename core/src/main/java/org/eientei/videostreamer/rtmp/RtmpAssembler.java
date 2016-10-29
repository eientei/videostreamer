package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eientei.videostreamer.rtmp.message.*;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class RtmpAssembler {
    private final ChannelHandlerContext ctx;
    private final RtmpDecoderHandler decoder;
    private final int chunk;

    private ByteBuf incompleteBuf;
    private RtmpMessageType lastType;
    private int lastStreamid;
    private int lastTime;
    private int lastLength;
    private int lastTimediff;


    public RtmpAssembler(ChannelHandlerContext ctx, RtmpDecoderHandler decoder, int chunk) {
        this.ctx = ctx;
        this.decoder = decoder;
        this.chunk = chunk;

        this.incompleteBuf = ctx.alloc().buffer();
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                incompleteBuf.release();
            }
        });
    }

    public void update(ByteBuf in, List<Object> out) {
        append(in);
        if (incompleteBuf.readableBytes() >= lastLength) {
            complete(out);
        }
    }

    private void append(ByteBuf in) {
        int remain = lastLength - incompleteBuf.readableBytes();
        if (remain > decoder.getChunkin()) {
            remain = decoder.getChunkin();
        }

        incompleteBuf.ensureWritable(remain);
        incompleteBuf.writeBytes(in, remain);
        decoder.updateRead(remain);
    }

    public void parseHeader(RtmpHeaderSize size, ByteBuf in) {
        switch (size) {
            case FULL:
                lastTime = in.readMedium();
                lastLength = in.readMedium();
                lastType = RtmpMessageType.dispatch(in.readByte());
                lastStreamid = in.readIntLE();
                if (lastTime == 0xFFFFFF) {
                    lastTime = in.readInt();
                }
                break;
            case MEDIUM:
                lastTimediff = in.readMedium();
                lastTime += lastTimediff;
                lastLength = in.readMedium();
                lastType = RtmpMessageType.dispatch(in.readByte());
                break;
            case SHORT:
                lastTimediff = in.readMedium();
                lastTime += lastTimediff;
                break;
            case NONE:
                if (incompleteBuf.readableBytes() == 0) {
                    lastTime += lastTimediff;
                }
                break;
        }
    }

    private void complete(List<Object> out) {
        switch (lastType) {
            case SET_CHUNK_SIZE:
                out.add(new RtmpSetChunkMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case ACK:
                out.add(new RtmpAckMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case USER:
                out.add(new RtmpUserMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case WINACK:
                out.add(new RtmpWinackMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case SET_PEER_BAND:
                out.add(new RtmpSetPeerBandMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case AUDIO:
                out.add(new RtmpAudioMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case VIDEO:
                out.add(new RtmpVideoMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case AMF3_CMD_ALT:
            case AMF3_CMD:
                incompleteBuf.skipBytes(1);
                out.add(new RtmpCmdMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case AMF3_META:
                incompleteBuf.skipBytes(1);
                out.add(new RtmpMetaMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case AMF0_META:
                out.add(new RtmpMetaMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            case AMF0_CMD:
                out.add(new RtmpCmdMessage(chunk, lastStreamid, lastTime, incompleteBuf));
                break;
            default:
                break;
        }

        incompleteBuf = ctx.alloc().buffer();
    }
}
