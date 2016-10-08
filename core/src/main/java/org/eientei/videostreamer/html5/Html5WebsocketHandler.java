package org.eientei.videostreamer.html5;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Html5WebsocketHandler(RtmpServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Html5WebsocketCommand cmd = objectMapper.readValue(payload, Html5WebsocketCommand.class);
        if ("play".equals(cmd.getAction())) {
            playStream(session, cmd.getStream());
        } else if ("stop".equals(cmd.getAction())) {
            stopStream(session);
        } else {
            session.close();
        }
    }

    private void stopStream(WebSocketSession session) {
        Html5RtmpClient client = (Html5RtmpClient) session.getAttributes().get("client");
        RtmpStreamContext stream = (RtmpStreamContext) session.getAttributes().get("stream");
        if (client == null) {
            return;
        }
        stream.unsubscribe(client);
        client.close();
        session.getAttributes().remove("client");
        session.getAttributes().remove("stream");
    }

    private void playStream(WebSocketSession session, String streamName) {
        Html5RtmpClient client = (Html5RtmpClient) session.getAttributes().get("client");
        if (client != null) {
            stopStream(session);
        }
        client = new Html5RtmpClient(session);
        RtmpStreamContext stream = serverContext.getStream(streamName);
        stream.subscribe(client);

        session.getAttributes().put("client", client);
        session.getAttributes().put("stream", stream);
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        stopStream(session);
    }
}
