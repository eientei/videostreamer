package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-26
 */
public class RtmpAmfMetaMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpAmfMetaMessage> PARSER = new RtmpMessageParser<RtmpAmfMetaMessage>() {
        @Override
        public RtmpAmfMetaMessage parse(RtmpUnchunkedMessage msg) {
            List<Object> values = new ArrayList<>();
            while (msg.getData().isReadable()) {
                Object obj = Amf.deserialize(msg.getData());
                values.add(obj);
            }

            return new RtmpAmfMetaMessage(values);
        }
    };
    private final List<Object> values;

    public RtmpAmfMetaMessage(List<Object> values) {
        super(5, 1, Type.AMF0_META);
        this.values = values;
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public void serialize(ByteBuf data) {
        for (Object obj : getValues()) {
            Amf.serialize(data, obj);
        }
    }

    @Override
    protected RtmpMessage dupInternal() {
        return new RtmpAmfMetaMessage(values);
    }
}
