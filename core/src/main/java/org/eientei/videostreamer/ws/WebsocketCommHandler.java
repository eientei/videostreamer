package org.eientei.videostreamer.ws;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.util.internal.ConcurrentSet;
import org.eientei.videostreamer.mp4.Mp4Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Set;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
@Component
public class WebsocketCommHandler extends AbstractWebSocketHandler {
    public static final String CONTEXT = "context";

    private final Set<WebSocketSession> sessions = new ConcurrentSet<>();
    private final JsonFactory jsonFactory = new JsonFactory();
    private final Mp4Server mp4Server;

    @Autowired
    public WebsocketCommHandler(Mp4Server mp4Server) {
        this.mp4Server = mp4Server;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.getAttributes().put(CONTEXT, new WebsocketCommContext(session, mp4Server));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        WebsocketCommContext context = (WebsocketCommContext) session.getAttributes().get(CONTEXT);
        context.close();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebsocketCommContext context = (WebsocketCommContext) session.getAttributes().get(CONTEXT);
        JsonParser parser = jsonFactory.createParser(message.getPayload());
        parser.nextToken();
        String action = parser.nextFieldName();
        String params = parser.nextTextValue();
        context.process(action, params);
    }
}
