package org.eientei.videostreamer.controller;

import org.eientei.videostreamer.config.security.VideostreamerUser;
import org.eientei.videostreamer.dto.PasswordDTO;
import org.eientei.videostreamer.dto.SignupDTO;
import org.eientei.videostreamer.dto.UserDTO;
import org.eientei.videostreamer.dto.Util;
import org.eientei.videostreamer.orm.error.AlreadyExists;
import org.eientei.videostreamer.orm.error.InvalidPassword;
import org.eientei.videostreamer.orm.error.TooManyStreams;
import org.eientei.videostreamer.orm.error.WrongPassword;
import org.eientei.videostreamer.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-05
 * Time: 18:42
 */
@RestController
@RequestMapping("security")
public class Security {
    @Autowired
    private UserService userService;

    @Autowired
    private Md5PasswordEncoder passwordEncoder;

    @RequestMapping("user")
    public UserDTO user(@AuthenticationPrincipal VideostreamerUser user, HttpServletRequest request) {
        return new UserDTO(user.getUsername(), determineHash(user, request), user.getEntity().getEmail());
    }

    @RequestMapping("signup")
    public void signup(@Valid @RequestBody SignupDTO dto) throws AlreadyExists, TooManyStreams {
        userService.createUser(dto.getUsername(), dto.getPassword(), dto.getEmail(), true);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("email")
    public String updateEmail(@AuthenticationPrincipal VideostreamerUser user, @RequestBody String email) {
        userService.updateEmail(user.getEntity(), email);
        return "\"" + Util.hash(user.getEntity()) + "\"";
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("password")
    public void updatePassword(@AuthenticationPrincipal VideostreamerUser user, @RequestBody PasswordDTO dto) throws WrongPassword, InvalidPassword {
        if (dto.getDesired() == null || dto.getDesired().length() < 3) {
            throw new InvalidPassword();
        }
        userService.updatePassword(user.getEntity(), dto.getCurrent(), dto.getDesired());
    }

    private String determineHash(VideostreamerUser user, HttpServletRequest request) {
        return Util.hash(user.getEntity(), determineIp(request));
    }

    private String determineIp(HttpServletRequest request) {
        String remote = request.getHeader("X-Forwarded-For");
        if (remote == null) {
            remote = request.getRemoteAddr();
        }
        return remote;
    }
}
