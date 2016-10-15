package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.rtmp.RtmpHeader;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpAmfMessage extends RtmpMessage {
    public RtmpAmfMessage(RtmpMessageType type, int chunkid, long streamid, long time, Object... data) {
        super(type, chunkid, streamid, time);
        for (Object d : data) {
            Amf.serialize(getData(), d);
        }
    }

    public RtmpAmfMessage(RtmpMessageType type, int chunkid, long streamid, long time, ByteBuf data) {
        super(type, chunkid, streamid, time, data);
    }

    public RtmpAmfMessage(RtmpHeader header, ByteBuf slice) {
        super(header, slice);
    }

    @Override
    public RtmpMessage copy() {
        return new RtmpAmfMessage(getHeader(), getData().retain().slice());
    }

    public AmfListWrapper getAmf() {
        List<Object> result = new ArrayList<>();
        ByteBuf slice = getData().slice();

        while (slice.isReadable()) {
            result.add(Amf.deserialize(slice));
        }

        return new AmfListWrapper(result);
    }
}
