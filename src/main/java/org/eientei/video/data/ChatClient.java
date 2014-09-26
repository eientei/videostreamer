package org.eientei.video.data;

import org.eientei.video.orm.entity.User;
import org.eientei.video.orm.util.VideostreamUtils;
import org.eientei.video.security.AppUserDetails;
import org.eientei.video.security.AppUserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketSession;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:33
 */
public class ChatClient implements Comparable<ChatClient> {
    private WebSocketSession session;
    private String remote;
    private String userHash;
    private ChatRoom.DescendingIterator messageIterator;
    private User user;
    private ChatRoom room;

    public ChatClient(WebSocketSession session, AppUserDetailsService userService) {
        AppUserDetails userDetails;
        if (session.getPrincipal() == null) {
            userDetails = (AppUserDetails) userService.loadUserByUsername("Anonymous");
        } else {
            userDetails = (AppUserDetails) ((Authentication) session.getPrincipal()).getPrincipal();
        }

        remote = session.getHandshakeHeaders().get("X-Real-IP").get(0);
        if (remote == null) {
            remote = session.getRemoteAddress().getAddress().getHostAddress();
        }
        this.session = session;
        this.user = userDetails.getDataUser();
        this.userHash = VideostreamUtils.determineUserHash(user, remote);
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getRemote() {
        return remote;
    }

    public String getUserHash() {
        return userHash;
    }

    public ChatRoom.DescendingIterator getMessageIterator() {
        return messageIterator;
    }

    public void setMessageIterator(ChatRoom.DescendingIterator messageIterator) {
        this.messageIterator = messageIterator;
    }

    public User getUser() {
        return user;
    }

    public void setRoom(ChatRoom room) {
        this.room = room;
    }

    public ChatRoom getRoom() {
        return room;
    }

    @Override
    public int compareTo(ChatClient o) {
        return getSession().getId().compareTo(o.getSession().getId());
    }
}
