package org.eientei.videostreamer.ws;

import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.server.ServerContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class WebsocketCommHandler extends AbstractWebSocketHandler {
    private final static String CONTEXT = "context";
    private final ServerContext globalContext;

    public WebsocketCommHandler(ServerContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Channel channel = new EmbeddedChannel(
                DefaultChannelId.newInstance(),
                new WebsocketCodecHandler(session),
                new WebsocketDecoderHandler(),
                new WebsocketMessageHandler(globalContext)
        );
        session.getAttributes().put(CONTEXT, channel);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Channel channel = (Channel) session.getAttributes().get(CONTEXT);
        channel.pipeline().fireChannelRead(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Channel channel = (Channel) session.getAttributes().get(CONTEXT);
        channel.close();
    }
}
