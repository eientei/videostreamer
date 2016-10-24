package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ConcurrentSet;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpMetaMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;

import java.util.Map;
import java.util.Set;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpStream {
    private final RtmpServer server;
    private final String name;

    private RtmpSubscriber publisher;
    private Set<RtmpSubscriber> subscribers = new ConcurrentSet<>();
    private AmfObjectWrapper metadata;
    private ByteBuf videoinit;
    private ByteBuf audioinit;

    private boolean wasaudio;
    private boolean wasvideo;
    private boolean booted;

    public RtmpStream(RtmpServer server, String name) {
        this.server = server;
        this.name = name;
    }

    public void broadcastAudio(RtmpAudioMessage rtmpAudioMessage) {
        if (wasaudio && videoinit == null) {
            videoinit = Unpooled.EMPTY_BUFFER;
            checkBooted();
        }
        if (audioinit == null && rtmpAudioMessage.getData().getByte(1) == 0) {
            audioinit = rtmpAudioMessage.getData().copy();
            checkBooted();
            return;
        }
        for (RtmpSubscriber subscriber : subscribers) {
            rtmpAudioMessage.getData().retain();
            subscriber.acceptAudio(rtmpAudioMessage.getData().slice(), rtmpAudioMessage.getTime());
        }
        wasaudio = true;
    }

    private void checkBooted() {
        booted = metadata != null && audioinit != null && videoinit != null;
        if (booted) {
            for (RtmpSubscriber client : subscribers) {
                boot(client);
            }
        }
    }

    private void boot(RtmpSubscriber client) {
        client.begin(metadata, videoinit.slice(), audioinit.slice());
    }

    public void broadcastVideo(RtmpVideoMessage rtmpVideoMessage) {
        if (wasvideo && audioinit == null) {
            audioinit = Unpooled.EMPTY_BUFFER;
            checkBooted();
        }
        if (videoinit == null && rtmpVideoMessage.getData().getByte(1) == 0) {
            videoinit = rtmpVideoMessage.getData().copy();
            checkBooted();
            return;
        }

        for (RtmpSubscriber subscriber : subscribers) {
            rtmpVideoMessage.getData().retain();
            subscriber.acceptVideo(rtmpVideoMessage.getData().slice(), rtmpVideoMessage.getTime());
        }
        wasvideo = true;
    }

    @SuppressWarnings("unchecked")
    public void broadcastMeta(RtmpMetaMessage rtmpMetaMessage) {
        AmfListWrapper amf = Amf.deserializeAll(rtmpMetaMessage.getData());
        Map<String, Object> data = amf.get(2);

        metadata = Amf.makeObject(
                "videocodecid", 0.0,
                "audiocodecid", 0.0,
                "videodatarate", data.get("videodatarate"),
                "audiodatarate", data.get("audiodatarate"),
                "duration", 0.0,
                "framerate", data.get("framerate"),
                "fps", data.get("framerate"),
                "width", data.get("width"),
                "height", data.get("height"),
                "displaywidth", data.get("width"),
                "height", data.get("height")
        );

        checkBooted();

        //for (RtmpSubscriber subscriber : subscribers) {
            //subscriber.acceptMeta(metadata, rtmpMetaMessage.getTime());
        //}
    }

    public void publish(RtmpSubscriber client) {
        if (publisher != null) {
            return;
        }
        publisher = client;
        for (RtmpPublishNotifier notifier : server.getNotifiers()) {
            notifier.publish(this);
        }
    }

    public void subscribe(RtmpSubscriber client) {
        if (booted) {
            boot(client);
        }
        subscribers.add(client);
    }

    public void unpublish(RtmpSubscriber owner) {
        if (publisher != owner) {
            return;
        }

        publisher = null;
        for (RtmpSubscriber subscriber : subscribers) {
            subscriber.finish();
        }

        if (videoinit != null && videoinit.refCnt() > 0) {
            videoinit.release();
        }

        if (audioinit != null && audioinit.refCnt() > 0) {
            audioinit.release();
        }

        wasaudio = false;
        wasvideo = false;

        booted = false;

        metadata = null;
        videoinit = null;
        audioinit = null;

        for (RtmpPublishNotifier notifier : server.getNotifiers()) {
            notifier.unpublish(this);
        }
    }

    public void unsubscribe(RtmpSubscriber context) {
        subscribers.remove(context);
    }

    public String getName() {
        return name;
    }
}
