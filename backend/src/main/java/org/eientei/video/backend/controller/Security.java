package org.eientei.video.backend.controller;

import org.eientei.video.backend.config.security.VideostreamerUser;
import org.eientei.video.backend.controller.model.ChatRoom;
import org.eientei.video.backend.dto.*;
import org.eientei.video.backend.orm.entity.User;
import org.eientei.video.backend.orm.error.*;
import org.eientei.video.backend.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-28
 * Time: 13:52
 */
@RestController
@RequestMapping("security")
public class Security {
    @Autowired
    private UserService userService;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Chat chat;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private FilterChainProxy filterChainProxy;

    @Value("${videostreamer.domain:}")
    private String domainName;

    private Filter userFilter;

    @PostConstruct
    public void postConstruct() {
        for (Filter filter : filterChainProxy.getFilters("/security/login")) {
            if (filter instanceof UsernamePasswordAuthenticationFilter) {
                userFilter = filter;
                break;
            }
        }
    }

    @RequestMapping(value = "user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO user(@AuthenticationPrincipal VideostreamerUser user, HttpServletRequest request) {
        return new UserDTO(user.getUsername(), Util.determineHash(user, request), user.getEntity().getEmail());
    }

    @RequestMapping(value = "login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO login(@Valid @RequestBody LoginDTO dto) {
        return null;
    }

    @RequestMapping(value = "logout", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO logout() {
        return null;
    }

    @RequestMapping(value = "signup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void signup(@Valid @RequestBody final SignupDTO dto, HttpServletRequest request, HttpServletResponse response) throws AlreadyExists, TooManyStreams {
        userService.createUser(dto.getUsername(), dto.getPassword(), dto.getEmail(), true);
        remotelogin(request, response, dto.getUsername(), dto.getPassword());
    }

    private void remotelogin(HttpServletRequest request, HttpServletResponse response, final String username, final String password) {
        try {
            userFilter.doFilter(new HttpServletRequestWrapper(request) {
                @Override
                public String getParameter(String name) {
                    if (name.equals("username")) {
                        return username;
                    } else if (name.equals("password")) {
                        return password;
                    }
                    return super.getParameter(name);
                }

                @Override
                public String getServletPath() {
                    return "/security/login";
                }

            }, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
        } catch (Exception ignore) {
        }
    }

    @RequestMapping(value = "resetreq", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void resetreq(@Valid @RequestBody PasswordResetRequestDTO dto, HttpServletRequest request) throws InvalidEmail, AlreadySent {
        User user = userService.getUserByEmail(dto.getUsername(), dto.getEmail());
        if (user == null) {
            throw new InvalidEmail();
        }
        String resetKey = userService.resetPassword(user);
        if (resetKey == null) {
            throw new AlreadySent();
        }
        String ip = Util.determineIp(request);
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

    @RequestMapping(value = "resettry", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean resettry(@RequestBody String resetkey) {
        return userService.getUserByResetKey(resetkey) != null;
    }

    @RequestMapping(value = "resetsubmit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void resetsubmit(@RequestBody PasswordResetDTO dto, HttpServletRequest request, HttpServletResponse response) throws InvalidPassword, WrongPassword, InvalidKey {
        User user = userService.getUserByResetKey(dto.getResetkey());
        if (user == null) {
            throw new InvalidKey();
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 3 || dto.getPassword().length() > 30) {
            throw new InvalidPassword();
        }

        userService.updatePassword(user, null, dto.getPassword());
        remotelogin(request, response, user.getName(), dto.getPassword());
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "email", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String email(@AuthenticationPrincipal VideostreamerUser user, @RequestBody String email) {
        userService.updateEmail(user.getEntity(), email);
        List<ChatRoom> rooms = chat.getRooms(user);
        for (ChatRoom room : rooms) {
            room.updateOnline();
        }

        return "\"" + Util.hash(user.getEntity()) + "\"";
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "password", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void password(@AuthenticationPrincipal VideostreamerUser user, @RequestBody PasswordDTO dto) throws WrongPassword, InvalidPassword {
        if (dto.getCurrent() == null || dto.getDesired() == null || dto.getDesired().length() < 3 || dto.getDesired().length() > 30) {
            throw new InvalidPassword();
        }
        userService.updatePassword(user.getEntity(), dto.getCurrent(), dto.getDesired());
    }
}
