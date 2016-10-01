package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.message.*;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public abstract class RtmpMessage {

    public enum Type {
        UNKNOWN0(0, RtmpNullMessage.PARSER),
        SET_CHUNK_SIZE(1, RtmpSetChunkSizeMessage.PARSER),
        ABORT(2, RtmpAbortMessage.PARSER),
        ACK(3, RtmpAckMessage.PARSER),
        USER(4, RtmpUserMessage.PARSER),
        WINACK(5, RtmpWinackMessage.PARSER),
        SET_PEER_BAND(6, RtmpSetPeerBandMessage.PARSER),
        EDGE(7, RtmpNullMessage.PARSER),
        AUDIO(8, RtmpAudioMessage.PARSER),
        VIDEO(9, RtmpVideoMessage.PARSER),
        UNKNOWN10(10, RtmpNullMessage.PARSER),
        AMF3_CMD_ALT(11, RtmpAmf3CmdMessage.PARSER),
        UNKNOWN12(12, RtmpNullMessage.PARSER),
        UNKNOWN13(13, RtmpNullMessage.PARSER),
        UNKNOWN14(14, RtmpNullMessage.PARSER),
        AMF3_META(15, RtmpAmfMetaMessage.PARSER),
        AMF3_SHARED(16, RtmpNullMessage.PARSER),
        AMF3_CMD(17, RtmpAmf3CmdMessage.PARSER),
        AMF0_META(18, RtmpAmfMetaMessage.PARSER),
        AMF0_SHARED(19, RtmpNullMessage.PARSER),
        AMF0_CMD(20, RtmpAmf0CmdMessage.PARSER),
        UNKNOWN21(21, RtmpNullMessage.PARSER),
        AGGREGATE(22, RtmpNullMessage.PARSER);
        
        private final int value;
        private final RtmpMessageParser<? extends RtmpMessage> parser;

        <T extends RtmpMessageParser<?>> Type(int value, T parser) {
            this.value = value;
            this.parser = parser;
        }

        public int getValue() {
            return value;
        }

        public RtmpMessageParser<? extends RtmpMessage> getParser() {
            return parser;
        }

        public static Type parseValue(int value) {
            if (value >= 0 || value < values().length) {
                return values()[value];
            }
            throw new IllegalArgumentException("Illegal RtmpMessage.Type value: " + value);
        }
    }


    private RtmpHeader header;

    public RtmpMessage(int chunkid, long streamId, Type type) {
        if (chunkid < 2) {
            chunkid = 2;
        }
        this.header = new RtmpHeader(chunkid, streamId, type);
    }

    public RtmpHeader getHeader() {
        return header;
    }

    public abstract void serialize(ByteBuf buf);
    protected abstract RtmpMessage dupInternal();

    public RtmpMessage dup(long offsetTime) {
        RtmpMessage msg = dupInternal();
        msg.getHeader().setFrom(getHeader());
        msg.getHeader().setTimestamp(offsetTime);
        return msg;
    }

    @Override
    public String toString() {
        return header.toString();
    }
}
