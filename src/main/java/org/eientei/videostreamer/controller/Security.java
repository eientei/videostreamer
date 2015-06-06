package org.eientei.videostreamer.controller;

import org.eientei.videostreamer.config.security.VideostreamerUser;
import org.eientei.videostreamer.dto.*;
import org.eientei.videostreamer.orm.entity.User;
import org.eientei.videostreamer.orm.error.*;
import org.eientei.videostreamer.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
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

    @Autowired
    private MailSender mailSender;

    @Value("${videostreamer.domain:}")
    private String domainName;

    @RequestMapping("user")
    public UserDTO user(@AuthenticationPrincipal VideostreamerUser user, HttpServletRequest request) {
        return new UserDTO(user.getUsername(), determineHash(user, request), user.getEntity().getEmail());
    }

    @RequestMapping("signup")
    public void signup(@Valid @RequestBody SignupDTO dto) throws AlreadyExists, TooManyStreams {
        userService.createUser(dto.getUsername(), dto.getPassword(), dto.getEmail(), true);
    }

    @RequestMapping("resetreq")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO dto, HttpServletRequest request) throws InvalidEmail, AlreadySent {
        User user = userService.getUserByEmail(dto.getName(), dto.getEmail());
        if (user == null) {
            throw new InvalidEmail();
        }
        String resetKey = userService.resetPassword(user);
        if (resetKey == null) {
            throw new AlreadySent();
        }
        String ip = determineIp(request);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom("keeper@" + domainName);
        message.setSubject(domainName + " password reset");
        message.setText("User with ip "
                + ip
                + " has requested resetting of password on your account ("
                + user.getName()
                + ") at "
                + domainName
                + "\n\n"
                + "In order to reset your password, proceed to the link:\n"
                + "http://"
                + domainName
                + "/security/reset/"
                + resetKey);
        mailSender.send(message);
    }

    @RequestMapping("resettry")
    public boolean tryPasswordReset(@RequestBody String resetKey) {
        return userService.getUserByResetKey(resetKey) != null;
    }

    @RequestMapping("resetsubmit")
    public void performPasswordReset(@RequestBody PasswordResetDTO dto) throws InvalidPassword, WrongPassword, InvalidKey {
        User user = userService.getUserByResetKey(dto.getResetkey());
        if (user == null) {
           throw new InvalidKey();
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 3 || dto.getPassword().length() > 30) {
            throw new InvalidPassword();
        }

        userService.updatePassword(user, null, dto.getPassword());
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
        if (dto.getCurrent() == null || dto.getDesired() == null || dto.getDesired().length() < 3 || dto.getDesired().length() > 30) {
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
