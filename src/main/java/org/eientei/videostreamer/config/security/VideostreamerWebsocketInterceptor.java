package org.eientei.videostreamer.config.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 18:47
 */
public class VideostreamerWebsocketInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        attributes.put("principal", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        attributes.put("sessionid", ((ServletServerHttpRequest) request).getServletRequest().getSession().getId());
        String first = request.getHeaders().getFirst("X-Forwarded-For");
        if (first == null) {
            first = request.getRemoteAddress().getAddress().getHostAddress();
        }
        attributes.put("remoteips", first);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
