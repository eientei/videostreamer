package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpUserMessage extends RtmpMessage {

    public enum Event {
        STREAM_BEGIN(0),
        STREAM_EOF(1),
        STREAM_DRY(2),
        SET_BUFFER_LENGTH(3),
        STREAM_IS_RECORDED(4),
        UNKNOWN5(5),
        PING_REQUEST(6),
        PING_RESPONSE(7);

        private final int value;

        Event(int value) {
            this.value = value;
        }

        public static Event dispatch(int value) {
            return Event.values()[value & 0x7];
        }

        public int getValue() {
            return value;
        }
    }
    
    public RtmpUserMessage(int chunkid, long streamid, long time, Event event, int arg1, int arg2) {
        super(RtmpMessageType.USER, chunkid, streamid, time);
        getData().writeShort(event.getValue()).writeInt(arg1);
        if (event == Event.SET_BUFFER_LENGTH) {
            getData().writeInt(arg2);
        }
    }

    public RtmpUserMessage(int chunkid, long streamid, long time, ByteBuf data) {
        super(RtmpMessageType.USER, chunkid, streamid, time, data);
    }

    public RtmpUserMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpUserMessage(getHeader(), getData().retain().slice());
    }


    public Event getEvent() {
        return Event.dispatch(getData().getShort(0));
    }

    public int getArg1() {
        return getData().getInt(2);
    }

    public int getArg2() {
        return getData().getInt(6);
    }
}
