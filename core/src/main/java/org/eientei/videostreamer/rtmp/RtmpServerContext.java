package org.eientei.videostreamer.rtmp;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@Component
public class RtmpServerContext {
    private Map<String, RtmpStreamContext> streams = new ConcurrentHashMap<>();

    public RtmpStreamContext getStream(String name) {
        if (!streams.containsKey(name)) {
            streams.put(name, new RtmpStreamContext(name, this));
        }
        return streams.get(name);
    }

}
