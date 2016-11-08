package org.eientei.videostreamer.impl.ws;

import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.core.StreamContext;
import org.eientei.videostreamer.impl.core.StreamEventListener;
import org.eientei.videostreamer.impl.core.StreamPubsubListener;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class WebSocketContext implements StreamEventListener, StreamPubsubListener {
    private final GlobalContext globalContext;
    private final WebSocketSession session;

    public WebSocketContext(GlobalContext globalContext, WebSocketSession session) {
        this.globalContext = globalContext;
        this.session = session;
        StreamContext baka = globalContext.stream("baka");
        if (baka != null) {
            play("baka");
        }
    }

    public void release() {

    }

    public void accept(TextMessage message) {

    }

    @Override
    public void play(String name) {
        try {
            session.sendMessage(new TextMessage("{\"action\": \"play\", \"stream\": \"" + name + "\"}"));
        } catch (IOException e) {
        }
    }

    @Override
    public void stop(String name) {
        try {
            session.sendMessage(new TextMessage("{\"action\": \"stop\", \"stream\": \"" + name + "\"}"));
        } catch (IOException e) {
        }
    }

    @Override
    public void peers(int num) {
        try {
            session.sendMessage(new TextMessage("{\"action\": \"subscribers\", \"count\": \"" + num + "\"}"));
        } catch (IOException e) {
        }
    }
}
