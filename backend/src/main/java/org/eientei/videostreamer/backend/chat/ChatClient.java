package org.eientei.videostreamer.backend.chat;

import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-20
 * Time: 10:43
 */
public class ChatClient implements Comparable<ChatClient> {
    private ChatRoom room;
    private WebSocketSession session;
    private AppUserDetails appUserDetails;
    private String remote;

    public ChatClient(WebSocketSession session, AppUserDetails appUserDetails) {
        this.session = session;
        this.appUserDetails = appUserDetails;

        List<String> realIps = session.getHandshakeHeaders().get("X-Real-IP");
        if (realIps != null && !realIps.isEmpty()) {
            remote = realIps.get(0);
        }
        if (remote == null) {
            remote = session.getRemoteAddress().getAddress().getHostAddress();
        }
    }

    public ChatRoom getRoom() {
        return room;
    }

    public void setRoom(ChatRoom room) {
        this.room = room;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public AppUserDetails getAppUserDetails() {
        return appUserDetails;
    }

    public String getRemote() {
        return remote;
    }

    @Override
    public int compareTo(ChatClient o) {
        return session.getId().compareTo(o.getSession().getId());
    }

    public void setAppUserDetails(AppUserDetails appUserDetails) {
        this.appUserDetails = appUserDetails;
    }
}
