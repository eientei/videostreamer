package org.eientei.video.controller;

import org.eientei.video.orm.util.VideostreamUtils;
import org.eientei.video.security.AppUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;

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

    @ModelAttribute("rtmpBase")
    private String getRtmpBase() throws NamingException {
        if (rtmpBase == null) {
            Context context = new InitialContext();
            //context = (Context)context.lookup("java:comp/env");
            rtmpBase = (String)context.lookup("jdbc/VideoRtmpBase");
        }
        return rtmpBase;
    }

    @ModelAttribute("userhash")
    public String getUserHash(HttpServletRequest request) {
        try {
            String remote = request.getHeader("X-Real-IP");
            if (remote == null) {
                remote = request.getRemoteAddr();
            }
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
