package org.eientei.videostreamer.rtmp;

import com.google.common.collect.ImmutableMap;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public class RtmpStreamContext {
    public enum State {
        IDLE,
        BOOTSTAPPING,
        RUNNING
    }

    private Logger log = LoggerFactory.getLogger(RtmpStreamContext.class);
    private List<RtmpClient> clients = new CopyOnWriteArrayList<>();
    private RtmpClient source = null;
    private RtmpAmfMetaMessage metadata = null;
    private RtmpVideoMessage bootframe = null;
    private RtmpAudioMessage bootaudio = null;
    private State state = State.IDLE;
    private String name;

    public RtmpStreamContext(String name, List<RtmpClient> autoclients) {
        this.clients.addAll(autoclients);
        this.name = name;
        for (RtmpClient client : autoclients) {
            client.init(name);
        }
    }

    public void publish(RtmpClient source) throws Exception {
        if (state != State.IDLE) {
            throw new Exception("Already publishing");
        }
        this.source = source;
        state = State.BOOTSTAPPING;
        if (source instanceof RtmpClientContext) {
            log.info("Client {} is now publishing on stream {}", ((RtmpClientContext) source).getId(), name);
        }
    }

    public void unpublish(RtmpClient source) throws Exception {
        if (source != this.source) {
            return;
        }
        if (state != State.IDLE) {
            this.source = null;
            this.metadata = null;
            this.bootframe = null;
            this.bootaudio = null;
            for (RtmpClient client : clients) {
                client.accept(new RtmpUserMessage(RtmpUserMessage.Event.STREAM_EOF, 1, 0));
            }
            state = State.IDLE;
            if (source instanceof RtmpClientContext) {
                log.info("Client {} is no longer publishing on stream {}", ((RtmpClientContext) source).getId(), name);
            }
        }
    }

    public void subscribe(RtmpClient client) {
        if (!clients.contains(client)) {
            if (state == State.RUNNING) {
                bootstrap(client);
            }
            clients.add(client);
            client.init(name);

            if (client instanceof RtmpClientContext) {
                log.info("Client {} is now subscribed for {}", ((RtmpClientContext) client).getId(), name);
            }
        }
    }

    public void unsubscribe(RtmpClient client) {
        if (clients.contains(client)) {
            clients.remove(client);
            if (client instanceof RtmpClientContext) {
                log.info("Client {} is now unsubscribed for {}", ((RtmpClientContext) client).getId(), name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void broadcastMetadata(RtmpAmfMetaMessage metadata) {
        Map<String, Object> data = (Map<String, Object>) metadata.getValues().get(2);
        Map<String, Object> map = new HashMap<>();
        map.put("videocodecid", 0.0);
        map.put("audiocodecid", 0.0);
        map.put("audiodatarate", data.get("audiodatarate"));
        map.put("videodatarate", data.get("videodatarate"));
        map.put("duration", 0.0);
        map.put("fps", data.get("framerate"));
        map.put("framerate", data.get("framerate"));
        map.put("width", data.get("width"));
        map.put("height", data.get("height"));
        map.put("displaywidth", data.get("width"));
        map.put("displayheight", data.get("height"));
        List<Object> values = new ArrayList<>();
        values.add("onMetaData");
        values.add(Amf.makeObject(ImmutableMap.copyOf(map)));
        this.metadata = new RtmpAmfMetaMessage(values);
        this.metadata.getHeader().setTimestamp(metadata.getHeader().getTimestamp());
        checkBootstrap();
        broadcast(this.metadata);
    }

    public void broadcastVideo(RtmpVideoMessage video) {
        if (video.getData()[1] == 0x00) {
            bootframe = video;
            if (state == State.BOOTSTAPPING) {
                checkBootstrap();
                return;
            }
        } else if (video.getData()[0] == 0x17) {
            if (state == State.BOOTSTAPPING) {
                checkBootstrap();
                return;
            }
        }
        broadcast(video);
    }

    private void checkBootstrap() {
        if (state == State.BOOTSTAPPING && metadata != null && bootframe != null && bootaudio != null) {
            for (RtmpClient client : clients) {
                bootstrap(client);
            }
            state = State.RUNNING;
        }
    }

    private synchronized void bootstrap(RtmpClient client) {
        client.accept(new RtmpUserMessage(RtmpUserMessage.Event.STREAM_BEGIN, 1, 0));

        List<Object> values = new ArrayList<>();
        values.add("onStatus");
        values.add(0.0);
        values.add(null);
        values.add(Amf.makeObject(ImmutableMap.builder()
                .put("level", "status")
                .put("code", "NetStream.Play.Start")
                .put("description", "Start live.")
                .build()));
        RtmpAmf0CmdMessage cmd = new RtmpAmf0CmdMessage(values);
        cmd.getHeader().setChunkid(5);
        cmd.getHeader().setStreamid(1);
        client.accept(cmd);

        List<Object> metavalues = new ArrayList<>();
        metavalues.add("|RtmpSampleAccess");
        metavalues.add(true);
        metavalues.add(true);
        RtmpAmfMetaMessage meta = new RtmpAmfMetaMessage(metavalues);
        meta.getHeader().setChunkid(5);
        meta.getHeader().setStreamid(1);
        client.accept(meta);

        values = new ArrayList<>();
        values.add("onStatus");
        values.add(0.0);
        values.add(null);
        values.add(Amf.makeObject(ImmutableMap.builder()
                .put("level", "status")
                .put("code", "NetStream.Play.PublishNotify")
                .put("description", "Start publishing")
                .build()));
        cmd = new RtmpAmf0CmdMessage(values);
        cmd.getHeader().setChunkid(5);
        cmd.getHeader().setStreamid(1);
        client.accept(cmd);

        log.info("boots {}", metadata);
        log.info("boots {}", bootframe);
        log.info("boots {}", bootaudio);
        client.accept(metadata.dup(0));
        client.accept(bootframe.dup(0));
        client.accept(bootaudio.dup(0));
        if (client instanceof RtmpClientContext) {
            log.info("Client {} is now bootstrapped on {}", ((RtmpClientContext) client).getId(), name);
        }
    }

    public void broadcastAudio(RtmpAudioMessage audio) {
        if (bootaudio == null) {
            bootaudio = audio;
        }
        broadcast(audio);
    }

    public synchronized void broadcast(RtmpMessage message) {
        if (state != State.RUNNING) {
            log.warn("dropping {}", message);
            return;
        }

        for (RtmpClient client : clients) {
            RtmpMessage d = message.dup(message.getHeader().getTimestamp());
            client.accept(d);
        }
    }
}
