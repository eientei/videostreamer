package org.eientei.videostreamer.config.security;

import org.eientei.videostreamer.orm.entity.User;
import org.eientei.videostreamer.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-03
 * Time: 13:30
 */
@Component
public class VideostreamerUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userService.getUserByUserName(s);
        if (user == null) {
            throw new UsernameNotFoundException(s);
        }
        String password = user.getPasswordhash();
        if (password == null) {
            password = "";
        }
        return new VideostreamerUser(user.getName(), password, user);
    }
}
