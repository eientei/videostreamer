package org.eientei.videostreamer.backend.security;


import org.eientei.videostreamer.backend.orm.entity.User;
import org.eientei.videostreamer.backend.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * User: iamtakingiteasy
 * Date: 2014-10-18
 * Time: 15:47
 */
@Component
public class AppUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        if (userName.equals("Anonymous")) {
            User user = userService.getUserByNameStrict(userName);
            if (user == null) {
                user = userService.createUser(userName, null, null);
            }
            return new AppUserDetails(userName, "", user, new HashSet<GrantedAuthority>());
        }

        User user = userService.getUserByNameStrict(userName);

        if (user == null) {
            throw new UsernameNotFoundException("User " + userName + " was not found");
        }

        String passwordHash = user.getPasswordhash();
        if (passwordHash == null) {
            passwordHash = "";
        }

        return new AppUserDetails(user.getName(), passwordHash, user, new HashSet<GrantedAuthority>());
    }
}
