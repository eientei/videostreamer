package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
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

        private int value;

        Event(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Event parseValue(int value) {
            if (value >= 0 || value < values().length) {
                return values()[value];
            }
            throw new IllegalArgumentException("Illegal RtmpUserMessage.Event value: " + value);
        }
    }

    public static final RtmpMessageParser<RtmpUserMessage> PARSER = new RtmpMessageParser<RtmpUserMessage>() {
        @Override
        public RtmpUserMessage parse(RtmpUnchunkedMessage msg) {
            Event event = Event.parseValue(msg.getData().readUnsignedShort());
            long first = msg.getData().readUnsignedInt();
            long second = 0;
            if (event == Event.SET_BUFFER_LENGTH) {
                second = msg.getData().readUnsignedInt();
            }
            return new RtmpUserMessage(event, first, second);
        }
    };
    private final Event event;
    private final long first;
    private final long second;

    public RtmpUserMessage(Event event, long first, long second) {
        super(0, 0, Type.USER);
        this.event = event;
        this.first = first;
        this.second = second;
    }

    public Event getEvent() {
        return event;
    }

    public long getFirst() {
        return first;
    }

    public long getSecond() {
        return second;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeShort(getEvent().getValue());
        data.writeInt((int) getFirst());
        data.writeInt((int) getSecond());
    }
}
