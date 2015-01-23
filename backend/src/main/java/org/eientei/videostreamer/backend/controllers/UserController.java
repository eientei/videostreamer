package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.errors.CaptchaVerificationError;
import org.eientei.videostreamer.backend.orm.entity.User;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.eientei.videostreamer.backend.orm.service.UserService;
import org.eientei.videostreamer.backend.pojo.user.Login;
import org.eientei.videostreamer.backend.pojo.user.Signup;
import org.eientei.videostreamer.backend.pojo.user.UpdateCredentials;
import org.eientei.videostreamer.backend.pojo.user.UpdatePersonal;
import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.eientei.videostreamer.backend.security.AppUserDetailsService;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-15
 * Time: 05:49
 */
@RequestMapping("user")
@Controller
public class UserController {
    @Autowired
    private ConfigBootstrap configBootstrap;

    @Autowired
    private UserService userService;

    @Autowired
    private StreamService streamService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RememberMeServices rememberMeServices;

    @Autowired
    private AppUserDetailsService appUserDetailsService;

    @Autowired
    private ChatController chatController;

    @RequestMapping(value = "signup", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAnonymous()")
    public Object signup(@RequestBody Signup signupData, HttpServletRequest req, HttpServletResponse res) {
        boolean captchaResult = VideostreamerUtils.checkCaptcha(req, signupData.getCaptchaChallenge(), signupData.getCaptchaResponse(), configBootstrap.getRecaptchaPrivate());
        if (!captchaResult) {
            throw new CaptchaVerificationError();
        }

        User user = userService.createUser(signupData.getName(), signupData.getPassword(), signupData.getEmail());
        streamService.allocateStream(user, "live");
        AppUserDetails userDetails = loginUser(req, res, signupData.getName(), signupData.getPassword());
        return VideostreamerUtils.makeUserInfo(userDetails, req);
    }

    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAnonymous()")
    public Object login(@RequestBody Login loginData, HttpServletRequest req, HttpServletResponse res) {
        return VideostreamerUtils.makeUserInfo(loginUser(req, res, loginData.getName(), loginData.getPassword()), req);
    }

    @RequestMapping(value = "logout", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object logout(HttpServletRequest req, HttpServletResponse res) {
        Authentication auth = new AnonymousAuthenticationToken("Anonymous", appUserDetailsService.loadUserByUsername("Anonymous"), Collections.singleton(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        rememberMeServices.loginFail(req,res);
        String sessionId = req.getSession().getId();
        chatController.updateLogin(sessionId, (AppUserDetails) auth.getPrincipal());
        return VideostreamerUtils.makeUserInfo((AppUserDetails) auth.getPrincipal(), req);
    }

    @RequestMapping(value = "updatepersonal", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updatepersonal(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody UpdatePersonal updatePersonal , HttpServletRequest req) {
        userService.updateEmail(appUserDetails.getDataUser(), updatePersonal.getEmail());
        return VideostreamerUtils.makeUserInfo(appUserDetails, req);
    }

    @RequestMapping(value = "updatecredentials", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updatecredentials(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody UpdateCredentials updateCredentials , HttpServletRequest req) {
        userService.updatePassword(appUserDetails.getDataUser(), updateCredentials.getOriginalPassword(), updateCredentials.getPassword());
        return VideostreamerUtils.makeUserInfo(appUserDetails, req);
    }

    private AppUserDetails loginUser(HttpServletRequest request, HttpServletResponse response, String username, String password) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);

        Authentication result = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(result);

        rememberMeServices.loginSuccess(request, response, result);
        String sessionId = request.getSession().getId();
        chatController.updateLogin(sessionId, (AppUserDetails) result.getPrincipal());

        return (AppUserDetails) result.getPrincipal();
    }
}
