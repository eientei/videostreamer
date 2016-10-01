package org.eientei.videostreamer.rtmp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@Component
public class RtmpServerContext {
    private Map<String, RtmpStreamContext> streams = new ConcurrentHashMap<>();
    private List<RtmpClient> autoclients = new CopyOnWriteArrayList<>();

    @Autowired(required = false)
    public RtmpServerContext(List<RtmpClient> autoclients) {
        this.autoclients.addAll(autoclients);
    }

    public RtmpStreamContext getStream(String name) {
        if (!streams.containsKey(name)) {
            streams.put(name, new RtmpStreamContext(name, autoclients));
        }
        return streams.get(name);
    }
}
