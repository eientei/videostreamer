package org.eientei.videostreamer.mp4;

import org.eientei.videostreamer.rtmp.RtmpPublishNotifier;
import org.eientei.videostreamer.rtmp.RtmpStream;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-21
 */
@Component
public class Mp4Server implements RtmpPublishNotifier {
    private Map<String, Mp4Context> contexts = new ConcurrentHashMap<>();

    @Override
    public void publish(RtmpStream stream) {
        stream.subscribe(getContext(stream.getName()));
    }

    @Override
    public void unpublish(RtmpStream stream) {
        stream.unsubscribe(getContext(stream.getName()));
    }

    public Mp4Context getContext(String name) {
        Mp4Context context = contexts.get(name);
        if (context == null) {
            context = new Mp4Context(name);
            contexts.put(name, context);
        }

        return context;
    }
}