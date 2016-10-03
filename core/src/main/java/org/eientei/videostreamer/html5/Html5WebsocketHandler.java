package org.eientei.videostreamer.html5;

import org.eientei.videostreamer.rtmp.RtmpServerContext;
import org.eientei.videostreamer.rtmp.RtmpStreamContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Created by Alexander Tumin on 2016-10-02
 */
public class Html5WebsocketHandler extends AbstractWebSocketHandler {
    private final RtmpServerContext serverContext;

    public Html5WebsocketHandler(RtmpServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String name = message.getPayload();
        RtmpStreamContext stream = serverContext.getStream(name);
        Html5RtmpClient client = new Html5RtmpClient(session);
        session.getAttributes().put("name", name);
        session.getAttributes().put("client", client);
        session.getAttributes().put("stream", stream);
        stream.subscribe(client);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Html5RtmpClient client = (Html5RtmpClient) session.getAttributes().get("client");
        RtmpStreamContext stream = (RtmpStreamContext) session.getAttributes().get("stream");
        if (stream != null && client != null) {
            stream.unsubscribe(client);
        }
    }
}
