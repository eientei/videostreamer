package org.eientei.videostreamer.controller.model;

import org.eientei.videostreamer.config.security.VideostreamerUser;
import org.springframework.web.socket.WebSocketSession;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 14:14
 */
public class ChatClient {
    private WebSocketSession session;
    private VideostreamerUser user;
    private ChatRoom room;
    private Long typed = 0L;

    public ChatClient(WebSocketSession session, VideostreamerUser user) {
        this.session = session;
        this.user = user;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public VideostreamerUser getUser() {
        return user;
    }

    public void setUser(VideostreamerUser user) {
        this.user = user;
    }

    public ChatRoom getRoom() {
        return room;
    }

    public void setRoom(ChatRoom room) {
        this.room = room;
    }

    public void markTyping() {
        typed = System.currentTimeMillis();
    }

    public boolean isTyping(long time) {
        return (time - typed) < 1000;
    }

    public boolean isOwner() {
        return room.getStream().getAuthor().getName().equals(user.getUsername());
    }

    public String getRemote() {
        return session.getAttributes().get("remoteips").toString();
    }
}
