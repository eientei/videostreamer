package org.eientei.videostreamer.config.security;

import org.eientei.videostreamer.orm.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 08:34
 */
public class VideostreamerUser extends org.springframework.security.core.userdetails.User {
    private User user;

    public VideostreamerUser(String username, String password, User user) {
        super(username, password, Collections.<GrantedAuthority>emptyList());
        this.user = user;
    }

    public User getEntity() {
        return user;
    }
}
