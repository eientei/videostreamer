package org.eientei.video.backend.config;

import org.eientei.video.backend.config.security.VideostreamerWebsocketInterceptor;
import org.eientei.video.backend.controller.Chat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 14:16
 */
@EnableWebSocket
@Configuration
public class WebSocketConfiguration implements WebSocketConfigurer {
    @Autowired
    private Chat chat;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(chat, "/chat").addInterceptors(new VideostreamerWebsocketInterceptor()).withSockJS();
    }
}
