package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public class RtmpAmfCmdMessage extends RtmpMessage {
    private final List<Object> values;

    public RtmpAmfCmdMessage(List<Object> values, Type type) {
        super(3, 0, type);
        this.values = values;
    }

    protected static List<Object> parseValues(RtmpUnchunkedMessage msg) {
        List<Object> values = new ArrayList<>();
        while (msg.getData().isReadable()) {
            Object obj = Amf.deserialize(msg.getData());
            values.add(obj);
        }
        return values;
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
}
