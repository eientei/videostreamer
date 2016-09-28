package org.eientei.videostreamer.rtmp;

import com.google.common.collect.ImmutableMap;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.message.RtmpAmfMetaMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public class RtmpStreamMetadata {
    private final int width;
    private final int height;
    private final int framerate;

    private final RtmpAmfMetaMessage message;

    public RtmpStreamMetadata(Map<String, Object> data) {
        this.width = ((Double) data.get("width")).intValue();
        this.height = ((Double) data.get("height")).intValue();
        this.framerate = ((Double) data.get("framerate")).intValue();

        List<Object> values = new ArrayList<>();
        values.add("onMetaData");
        values.add(Amf.makeObject(ImmutableMap.builder()
                .put("width", width)
                .put("height", height)
                .put("framerate", framerate)
                .put("displayWidth", width)
                .put("displayHeight", height)
                .put("duration", -1.0)
                .put("videocodecid", 7.0)
                .put("audiocodecid", 10.0)
                .build()));
        this.message = new RtmpAmfMetaMessage(values);
        message.getHeader().setChunkid(3);
        message.getHeader().setStreamid(1);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFramerate() {
        return framerate;
    }

    public RtmpAmfMetaMessage getMessage() {
        return message;
    }
}
