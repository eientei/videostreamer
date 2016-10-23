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
        Mp4Context context = contexts.get(stream.getName());
        if (context == null) {
            context = new Mp4Context();
            contexts.put(stream.getName(), context);
        }
        stream.subscribe(context);
    }

    @Override
    public void unpublish(RtmpStream stream) {
        Mp4Context context = contexts.remove(stream.getName());
        stream.unsubscribe(context);
    }

    public Mp4Context getContext(String name) {
        return contexts.get(name);
    }
}
