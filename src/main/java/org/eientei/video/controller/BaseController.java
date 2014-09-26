package org.eientei.video.controller;

import org.eientei.video.orm.util.VideostreamUtils;
import org.eientei.video.security.AppUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-25
 * Time: 09:00
 */
public class BaseController {
    private String rtmpBase;

    private Boolean captcha;
    private String recaptchaPrivate;
    private String recaptchaPublic;

    @PostConstruct
    public void postConstruct() {
        try {
            Context context = new InitialContext();
            try {
                captcha = Boolean.parseBoolean((String) context.lookup("jdbc/VideoCaptcha"));
                recaptchaPrivate = (String) context.lookup("jdbc/VideoReCaptchaPrivate");
                recaptchaPublic = (String) context.lookup("jdbc/VideoReCaptchaPublic");
            } catch (Exception ignore) {
                captcha = false;
            }
            try {
                rtmpBase = (String) context.lookup("jdbc/VideoRtmpBase");
            } catch (Exception ignore) {
                rtmpBase = "";
            }
        } catch (Exception ignore) {
        }
    }

    @ModelAttribute("captcha")
    public Boolean getCaptchaEnabled() {
        return captcha;
    }

    public String getRecaptchaPrivate() {
        return recaptchaPrivate;
    }

    @ModelAttribute("reCaptchaPublic")
    public String getRecaptchaPublic() {
        return recaptchaPublic;
    }

    @ModelAttribute("rtmpBase")
    public String getRtmpBase() throws NamingException {
        return rtmpBase;
    }

    @ModelAttribute("userhash")
    public String getUserHash(HttpServletRequest request) {
        try {
            String remote = VideostreamUtils.getIp(request);
            AppUserDetails userDetails = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return VideostreamUtils.determineUserHash(userDetails.getDataUser(), remote);
        } catch (Exception e) {
            return "";
        }
    }

    @ModelAttribute("streamtitle")
    public String getStreamtitle() {
        return "";
    }

    @ModelAttribute("streameditable")
    public Boolean getStreameditable() {
        return false;
    }

    @ModelAttribute("showtitle")
    public Boolean getShowtitle() {
        return false;
    }
}
