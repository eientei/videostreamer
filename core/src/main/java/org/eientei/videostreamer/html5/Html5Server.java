package org.eientei.videostreamer.html5;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-09-29
 */
@Component
public class Html5Server {
    private final Map<String, Html5Stream> streams = new ConcurrentHashMap<>();

    public Html5Stream getStream(String streamName) {
        if (!streams.containsKey(streamName)) {
            streams.put(streamName, new Html5Stream(this));
        }
        return streams.get(streamName);
    }
}
