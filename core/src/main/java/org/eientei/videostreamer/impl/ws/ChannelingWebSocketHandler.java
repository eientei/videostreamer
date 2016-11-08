package org.eientei.videostreamer.impl.ws;

import org.eientei.videostreamer.impl.core.GlobalContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class ChannelingWebSocketHandler extends TextWebSocketHandler {
    private final GlobalContext globalContext;
    private final Map<String, WebSocketContext> contexts = new ConcurrentHashMap<>();

    public ChannelingWebSocketHandler(GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketContext context = new WebSocketContext(globalContext, session);
        globalContext.addEventListner(context);
        contexts.put(session.getId(), context);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        contexts.get(session.getId()).accept(message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebSocketContext context = contexts.remove(session.getId());
        globalContext.removeEventListener(context);
        context.release();
    }
}
