package org.eientei.videostreamer.websock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.videostreamer.rtmp.server.RtmpServer;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class WebsocketHandler extends AbstractWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RtmpServer server;

    public WebsocketHandler(RtmpServer server) {
        this.server = server;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        WebsocketCommand cmd = objectMapper.readValue(payload, WebsocketCommand.class);
        WebsocketClient client = (WebsocketClient) session.getAttributes().get("client");
        if (client == null) {
            client = new WebsocketClient(session, server.acquireStream(cmd.getStream()));
            session.getAttributes().put("client", client);
        }
        switch (cmd.getAction()) {
            case "play":
                playStream(client);
            break;
            case "stop":
                stopStream(client);
            break;
        }
    }

    private void stopStream(WebsocketClient client) {
        if (client.getRtmpStream() != null) {
            client.getRtmpStream().cleanup(client);
        }
        try {
            client.getSession().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playStream(WebsocketClient client) {
        client.getRtmpStream().subscribe(client);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebsocketClient client = (WebsocketClient) session.getAttributes().get("client");
        if (client != null) {
            stopStream(client);
        }
    }
}
