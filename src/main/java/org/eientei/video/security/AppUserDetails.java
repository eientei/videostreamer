package org.eientei.video.security;
;import org.eientei.video.orm.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:42
 */
public class AppUserDetails extends org.springframework.security.core.userdetails.User {
    private User dataUser;

    public AppUserDetails(String username, String password, User dataUser, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, !username.equals("Anonymous"), authorities);
        this.dataUser = dataUser;
    }

    public User getDataUser() {
        return dataUser;
    }
}
