package org.eientei.video.controller;

import org.eientei.video.form.LoginForm;
import org.eientei.video.form.ProfileForm;
import org.eientei.video.form.SignupForm;
import org.eientei.video.orm.entity.Stream;
import org.eientei.video.orm.entity.User;
import org.eientei.video.orm.service.StreamService;
import org.eientei.video.orm.service.UserService;
import org.eientei.video.orm.util.VideostreamUtils;
import org.eientei.video.security.AppUserDetails;
import org.eientei.video.security.SessionExpirer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-25
 * Time: 22:22
 */
@Controller
@RequestMapping("")
public class UserController extends BaseController {
    @Autowired
    private StreamService streamService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RememberMeServices rememberMeServices;

    @Autowired
    private SessionExpirer sessionExpirer;

    @Autowired
    private ChatController chatController;

    @RequestMapping("login")
    public String login(Model model) {
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            model.asMap().clear();
            return "redirect:/";
        }
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(HttpServletRequest request,
                        HttpServletResponse response,
                        Model model,
                        @Valid LoginForm loginForm,
                        BindingResult bindingResult,
                        @RequestParam(value = "recaptcha_challenge_field", required = false) String cChall,
                        @RequestParam(value = "recaptcha_response_field", required = false) String cResp) throws IOException, ServletException {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        if (!verifyCaptcha(request, cChall, cResp)) {
            bindingResult.addError(new ObjectError("loginForm", "Wrong captcha"));
            return "login";
        }

        try {
            loginUser(request, response, loginForm.getUsername(), loginForm.getPassword());
        } catch (Exception e) {
            bindingResult.addError(new ObjectError("loginForm", "Wrong login or password"));
            return "login";
        }

        model.asMap().clear();
        return "redirect:/profile";
    }

    @RequestMapping("signup")
    public String signup(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    private boolean verifyCaptcha(HttpServletRequest request, String cChall, String cResp) {
        if (getCaptchaEnabled()) {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, String> r = new LinkedMultiValueMap<String, String>();
            r.add("privatekey", getRecaptchaPrivate());
            r.add("remoteip",  VideostreamUtils.getIp(request));
            r.add("challenge", cChall);
            r.add("response", cResp);
            String res = restTemplate.postForObject("http://www.google.com/recaptcha/api/verify", r, String.class);

            if (!res.startsWith("true")) {
                return false;
            }
        }
        return true;
    }

    @RequestMapping(value = "signup", method = RequestMethod.POST)
    public String signup(HttpServletRequest request,
                         HttpServletResponse response,
                         Model model,
                         @Valid SignupForm signupForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "recaptcha_challenge_field", required = false) String cChall,
                         @RequestParam(value = "recaptcha_response_field", required = false) String cResp) throws IOException, ServletException {
        if (bindingResult.hasErrors()) {
            return "signup";
        }
        if (!signupForm.getPassword().equals(signupForm.getPasswordRepeat())) {
            bindingResult.addError(new FieldError("signupForm", "passwordRepeat", "Passwords do not match"));
            return "signup";
        }

        if (!verifyCaptcha(request, cChall, cResp)) {
            bindingResult.addError(new ObjectError("signupForm", "Wrong captcha"));
            return "signup";
        }

        User user;

        try {
            user = userService.createUser(signupForm.getUsername(), signupForm.getPassword(), signupForm.getEmail());
        } catch (UserService.UserAlreadyExists e) {
            bindingResult.addError(new ObjectError("signupForm", "Such username already exists"));
            return "signup";
        }

        allocateStream(signupForm.getUsername(), user);

        loginUser(request, response, signupForm.getUsername(), signupForm.getPassword());
        model.asMap().clear();
        return "redirect:/profile";
    }

    private void loginUser(HttpServletRequest request, HttpServletResponse response, String username, String password) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);

        Authentication result = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(result);

        rememberMeServices.loginSuccess(request, response, result);
        sessionExpirer.expireSession(request);
    }

    private Stream allocateStream(String streamname, User user) {
        try {
            return streamService.allocateStream("live", streamname, user);
        } catch (StreamService.StreamExists streamExists) {
            int i = 1;
            while (true) {
                String sname = streamname + i;
                i++;
                try {
                    return streamService.allocateStream("live", sname, user);
                } catch (Exception ignore) {
                }
            }
        } catch (StreamService.StreamInavlidName ignore) {
        }
        return null;
    }


    @RequestMapping("profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model) {
        populateProfileModel(model);
        return "profile";
    }

    @RequestMapping(value = "profile", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    public String profile(HttpServletRequest request, HttpServletResponse response, Model model, @Valid ProfileForm profileForm, BindingResult bindingResult) {
        String action = profileForm.getAction();
        AppUserDetails appUserDetails = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (action.equals("password")) {
            if (!appUserDetails.getDataUser().getPasswordhash().equals(VideostreamUtils.hashMd5(profileForm.getPasswordOriginal()))) {
                bindingResult.addError(new FieldError("profileForm", "passwordOriginal", "Incorrect current password"));
                return "profile";
            }
            if (profileForm.getPassword() == null || profileForm.getPassword().trim().isEmpty()) {
                bindingResult.addError(new FieldError("profileForm", "password", "May not be blank"));
                return "profile";
            }

            if (!profileForm.getPassword().equals(profileForm.getPasswordRepeat())) {
                bindingResult.addError(new FieldError("profileForm", "passwordRepeat", "Does not match"));
                return "profile";
            }

            appUserDetails.getDataUser().setPasswordhash(VideostreamUtils.hashMd5(profileForm.getPassword()));
            userService.saveUser(appUserDetails.getDataUser());
            loginUser(request, response, appUserDetails.getUsername(), profileForm.getPassword());
        } else if (action.equals("email")) {
            appUserDetails.getDataUser().setEmail(profileForm.getEmail());
            userService.saveUser(appUserDetails.getDataUser());
            model.addAttribute("userhash", getUserHash(request));
        } else if (action.equals("streamNameUpdate")) {
            long id = profileForm.getStream().getId();
            try {
                streamService.updateStreamName(id, appUserDetails.getDataUser(), profileForm.getStream().getName());
            } catch (StreamService.StreamExists streamExists) {
                bindingResult.addError(new FieldError("profileForm", "stream.name", "Such stream already exists"));
                Stream oldStream = streamService.getUserStream(appUserDetails.getDataUser());
                profileForm.setStream(oldStream);
                return "profile";
            } catch (StreamService.StreamInavlidName streamInavlidName) {
                bindingResult.addError(new FieldError("profileForm", "stream.name", streamInavlidName.getMessage()));
                profileForm.getStream().setName(streamService.getStreamByIdFor(id, appUserDetails.getDataUser()).getName());
                return "profile";
            }
        } else if (action.equals("streamTopicUpdate")) {
            streamService.updateStreamTopic(profileForm.getStream().getTopic(), appUserDetails.getDataUser());
        } else if (action.equals("streamToken")) {
            long id = profileForm.getStream().getId();
            streamService.updateStreamToken(id, appUserDetails.getDataUser());
        }
        model.asMap().clear();
        return "redirect:/profile";
    }

    private void populateProfileModel(Model model) {
        AppUserDetails userDetails = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Stream streams = streamService.getUserStream(userDetails.getDataUser());
        model.addAttribute("profileForm", new ProfileForm(userDetails.getDataUser().getEmail(), streams));
    }
}
