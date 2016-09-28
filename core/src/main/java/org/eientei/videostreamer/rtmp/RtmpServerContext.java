package org.eientei.videostreamer.rtmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@Component
public class RtmpServerContext {
    private Logger log = LoggerFactory.getLogger(RtmpServerContext.class);

    private Map<String, RtmpClientContext> connections = new ConcurrentHashMap<>();
    private Map<String, RtmpStream> streams = new ConcurrentHashMap<>();

    public void close() {
        for (RtmpClientContext conn : connections.values()) {
            conn.close();
        }
    }

    public RtmpStream acquireStream(String streamName) {
        if (!streams.containsKey(streamName)) {
            streams.put(streamName, new RtmpStream(this));
        }

        return streams.get(streamName);
    }

    public void releaseStream(String streamName) {
        streams.remove(streamName);
    }

    public void connect(RtmpClientContext connectionContext) {
        log.info("RTMP client {} connected", connectionContext.getId());
        connections.put(connectionContext.getId(), connectionContext);
    }

    public void disconnect(RtmpClientContext connectionContext) {
        log.info("RTMP client {} disconnected", connectionContext.getId());
        connections.remove(connectionContext.getId());
    }
}
