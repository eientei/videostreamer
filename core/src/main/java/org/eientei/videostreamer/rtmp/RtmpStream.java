package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.rtmp.message.RtmpAmfMessage;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpUserMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;
import org.eientei.videostreamer.rtmp.server.RtmpClient;
import org.eientei.videostreamer.rtmp.server.RtmpServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpStream {
    private final RtmpServer server;
    private final String name;

    private RtmpMessageAcceptor publisher;
    private List<RtmpMessageAcceptor> subscribers = new CopyOnWriteArrayList<>();
    private ByteBuf videoAvcFrame;
    private AmfObjectWrapper metadata;
    private ByteBuf audioFrame;

    public RtmpStream(RtmpServer server, String name) {

        this.server = server;
        this.name = name;
    }

    public synchronized boolean publish(RtmpMessageAcceptor client) {
        if (publisher != null) {
            return false;
        }

        publisher = client;

        for (RtmpMessageAcceptor subscriber : subscribers) {
            start(subscriber);
        }

        return true;
    }

    public synchronized boolean subscribe(RtmpMessageAcceptor client) {
        if (subscribers.contains(client)) {
            return false;
        }
        tryBoot(client);
        subscribers.add(client);
        return true;
    }

    public synchronized void cleanup(RtmpMessageAcceptor client) {
        if (publisher == client) {
            unpublish();
        }
        if (subscribers.contains(client)) {
            subscribers.remove(client);
            stop(client);
        }
    }

    private synchronized void unpublish() {
        publisher = null;
        for (RtmpMessageAcceptor subscriber : subscribers) {
            stop(subscriber);
        }

        videoAvcFrame = null;
        audioFrame = null;
        metadata = null;
    }

    public synchronized void broadcast(RtmpMessage message) {
        boolean dispose = false;
        if (message instanceof RtmpAmfMessage) {
            AmfListWrapper amf = ((RtmpAmfMessage) message).getAmf();
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

            message = new RtmpAmfMessage(RtmpMessageType.AMF0_META, 5, 1, 0, "onMetaData", metadata);
            dispose = true;
        } else if (message instanceof RtmpVideoMessage) {
            int fmt = message.getData().getByte(0);
            int frametype = (fmt & 0xf0) >> 4;
            int avcpacktype = message.getData().getByte(1);

            if (avcpacktype == 0) {
                if (videoAvcFrame != null) {
                    videoAvcFrame.release();
                }
                videoAvcFrame = message.getData().copy();
            }
        } else if (message instanceof RtmpAudioMessage) {
            if (message.getData().getByte(1) == 0) {
                if (audioFrame != null) {
                    audioFrame.release();
                }
                audioFrame = message.getData().copy();
            }
        }

        for (RtmpMessageAcceptor client : subscribers) {
            client.accept(message.copy());
        }

        if (dispose) {
            message.release();
        }
    }

    private void stop(RtmpMessageAcceptor client) {
        client.accept(new RtmpUserMessage(2, 0, 0, RtmpUserMessage.Event.STREAM_EOF, 1, 0));
    }


    private void start(RtmpMessageAcceptor client) {
        client.accept(new RtmpUserMessage(2, 0, 0, RtmpUserMessage.Event.STREAM_BEGIN, 1, 0));
        tryBoot(client);
    }

    private void tryBoot(RtmpMessageAcceptor client) {
        if (metadata != null) {
            client.accept(new RtmpAmfMessage(RtmpMessageType.AMF0_META, 5, 1, 0, "onMetaData", metadata));
        }

        if (videoAvcFrame != null) {
            client.accept(new RtmpVideoMessage(6, 1, 0, videoAvcFrame.copy()));
        }

        if (audioFrame != null) {
            client.accept(new RtmpAudioMessage(4, 1, 0, audioFrame.copy()));
        }

    }

    public boolean isPublisher(RtmpClient client) {
        return client == publisher;
    }
}
