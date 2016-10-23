package org.eientei.videostreamer.ws;

import org.eientei.videostreamer.mp4.Mp4Server;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class WebsocketLiveHandler extends AbstractWebSocketHandler {
    private static final String CONTEXT = "context";

    private final Mp4Server mp4Server;

    public WebsocketLiveHandler(Mp4Server mp4Server) {
        this.mp4Server = mp4Server;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.getAttributes().put(CONTEXT, new WebsocketLiveContext(session, mp4Server));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebsocketLiveContext context = (WebsocketLiveContext) session.getAttributes().get(CONTEXT);
        context.process(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebsocketLiveContext context = (WebsocketLiveContext) session.getAttributes().get(CONTEXT);
        context.close();
    }
}
