package org.eientei.videostreamer.impl.core;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.ConcurrentSet;
import org.eientei.videostreamer.impl.exceptions.StreamAlreadyPublishingException;
import org.eientei.videostreamer.impl.exceptions.StreamAlreadySubscribedException;
import org.eientei.videostreamer.impl.handlers.RtmpMessageDisposerHandler;
import org.eientei.videostreamer.impl.handlers.RtmpMessageInboundBroadcastHandler;
import org.eientei.videostreamer.impl.util.NamedThreadFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
@Component
public class GlobalContext {
    private final EventExecutor executor = new DefaultEventExecutor(new NamedThreadFactory("RTMP-RESTREAM"));
    private Map<String, StreamContext> streams = new HashMap<>();
    private Set<StreamEventListener> streamEventListeners = new ConcurrentSet<>();
    private Set<StreamPubsubListener> streamPubsubListeners = new ConcurrentSet<>();

    private StreamContext getOrCreateStream(String name) {
        StreamContext context = streams.get(name);
        if (context == null) {
            streams.put(name, context = new StreamContext(this, name, executor));
        }
        return context;
    }

    public void publish(final String name, final Channel channel) throws StreamAlreadyPublishingException {
        final StreamContext context = getOrCreateStream(name);
        if (context.getPublisher() != null) {
            throw new StreamAlreadyPublishingException();
        }

        context.setPublisher(channel);
        channel.pipeline().addLast(new RtmpMessageInboundBroadcastHandler(context));
        channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                context.setPublisher(null);

                if (context.sizeRtmpSubscribers() == 0) {
                    streams.remove(name).release();
                }

                for (StreamEventListener listener : streamEventListeners) {
                    listener.stop(name);
                }
            }
        });

        for (StreamEventListener listener : streamEventListeners) {
            listener.play(name);
        }
    }

    public void subscribe(final String name, final Channel channel) throws StreamAlreadySubscribedException {
        final StreamContext context = getOrCreateStream(name);
        if (context.containsRtmpSubscriber(channel)) {
            throw new StreamAlreadySubscribedException();
        }
        context.addRtmpSubscriber(channel);
        channel.pipeline().addLast(new RtmpMessageDisposerHandler());
        channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                context.removeRtmpSubscriber(channel);
                if (context.sizeRtmpSubscribers() == 0 && context.getPublisher() == null) {
                    streams.remove(name).release();
                }
            }
        });
    }

    public Set<StreamContext> allStreams() {
        return new HashSet<>(streams.values());
    }

    public StreamContext stream(String name) {
        return streams.get(name);
    }

    public void addEventListner(StreamEventListener listener) {
        streamEventListeners.add(listener);
    }

    public void removeEventListener(StreamEventListener listener) {
        streamEventListeners.remove(listener);
    }

    public void addPubsubListener(StreamPubsubListener listener) {
        streamPubsubListeners.add(listener);
    }

    public void removePubsubListener(StreamPubsubListener listener) {
        streamPubsubListeners.remove(listener);
    }

    public Set<StreamPubsubListener> getStreamPubsubListeners() {
        return streamPubsubListeners;
    }
}
