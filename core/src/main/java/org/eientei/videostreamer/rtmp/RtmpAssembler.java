package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.eientei.videostreamer.rtmp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpAssembler {
    private final Logger log = LoggerFactory.getLogger(RtmpAssembler.class);
    private final int chunk;
    private final RtmpContext context;

    private CompositeByteBuf incompleteData;

    private RtmpMessageType lastType;
    private int lastStreamid;
    private int lastTime;
    private int lastLength;
    private int lastTimediff;

    public RtmpAssembler(int chunk, RtmpContext context) {
        this.chunk = chunk;
        this.context = context;
        this.incompleteData = context.ALLOC.compose();
    }

    public void update(ByteBuf in) {
        append(in);
        if (incompleteData.readableBytes() >= lastLength) {
            if (incompleteData.readableBytes() != lastLength) {
                log.warn("{}: too much data", this.context);
            }
            complete();
        }
    }

    public void relase() {
        incompleteData.release();
    }

    private void complete() {
        switch (lastType) {
            case SET_CHUNK_SIZE:
                context.process(new RtmpSetChunkMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case ACK:
                context.process(new RtmpAckMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case USER:
                context.process(new RtmpUserMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case WINACK:
                context.process(new RtmpWinackMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case SET_PEER_BAND:
                context.process(new RtmpSetPeerBandMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case AUDIO:
                context.process(new RtmpAudioMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case VIDEO:
                context.process(new RtmpVideoMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case AMF3_CMD_ALT:
            case AMF3_CMD:
                incompleteData.skipBytes(1);
                context.process(new RtmpCmdMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case AMF3_META:
                incompleteData.skipBytes(1);
                context.process(new RtmpMetaMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case AMF0_META:
                context.process(new RtmpMetaMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            case AMF0_CMD:
                context.process(new RtmpCmdMessage(chunk, lastStreamid, lastTime, incompleteData));
                break;
            default:
                log.info("{}: unknown message type {}", context, lastType);
                break;
        }
        incompleteData.release();
        incompleteData = context.ALLOC.compose();
    }

    private void append(ByteBuf in) {
        int remain = lastLength - incompleteData.readableBytes();
        if (remain > context.getChunkin()) {
            remain = context.getChunkin();
        }

        ByteBuf copy = in.copy(in.readerIndex(), remain);
        incompleteData.addComponent(true, copy);
        in.skipBytes(remain);
        context.updateRead(remain);
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
                if (incompleteData.numComponents() == 0) {
                    lastTime += lastTimediff;
                }
                break;
        }
    }
}
