package org.eientei.video.security;

import org.eientei.video.orm.entity.Group;
import org.eientei.video.orm.entity.Role;
import org.eientei.video.orm.entity.User;
import org.eientei.video.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-14
 * Time: 16:59
 */
@Component
public class AppUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userService.getUserByName(userName);

        if (user == null) {
            if (userName.equals("Anonymous")) {
                try {
                    user = userService.createUser(userName, "", "");
                } catch (UserService.UserAlreadyExists e) {
                    throw new UsernameNotFoundException("Anonymous creation failed", e);
                }
            } else {
                throw new UsernameNotFoundException("User " + userName + " was not found");
            }
        }

        String passwordHash = user.getPasswordhash();
        if (passwordHash == null) {
            passwordHash = "";
        }

        Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
        for (Group group : user.getGroups()) {
            for (Role role : group.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
            }
        }
        return new AppUserDetails(user.getName(), passwordHash, user, authorities);
    }
}
