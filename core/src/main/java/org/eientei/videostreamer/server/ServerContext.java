package org.eientei.videostreamer.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import org.eientei.videostreamer.util.NamedThreadFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
@Component
public class ServerContext {
    private final EventExecutor rtmpExecutor = new DefaultEventExecutor(new NamedThreadFactory("RTMP-STREAM"));
    private final EventExecutor remuxExecutor = new DefaultEventExecutor(new NamedThreadFactory("RTMP-REMUXER"));

    private final Map<String, StreamContext> streams = new ConcurrentHashMap<>();

    public boolean publishRtmp(String name, Channel channel) {
        StreamContext streamContext = getStreamContext(name);
        return streamContext.publishRtmp(channel);
    }

    public boolean unpublishRtmp(String name, Channel channel) {
        StreamContext streamContext = getStreamContext(name);
        return streamContext.unpublishRtmp(channel);
    }

    public boolean subscribeRtmp(String subname, Channel channel) {
        StreamContext streamContext = getStreamContext(subname);
        return streamContext.subscrieRtmp(channel);
    }

    public boolean unsubscribeRtmp(String subname, Channel channel) {
        StreamContext streamContext = getStreamContext(subname);
        return streamContext.unsubscrieRtmp(channel);
    }

    public boolean subscribeMuxer(String name, Channel channel) {
        StreamContext streamContext = getStreamContext(name);
        return streamContext.subscribeMuxer(channel);
    }

    public boolean unsubscribeMuxer(String name, Channel channel) {
        StreamContext streamContext = getStreamContext(name);
        return streamContext.unsubscribeMuxer(channel);
    }

    private StreamContext getStreamContext(String name) {
        StreamContext context = streams.get(name);
        if (context == null) {
            streams.put(name, context = new StreamContext(this));
        }
        return context;
    }

    public EventExecutor getRtmpExecutor() {
        return rtmpExecutor;
    }

    public EventExecutor getRemuxExecutor() {
        return remuxExecutor;
    }
}
