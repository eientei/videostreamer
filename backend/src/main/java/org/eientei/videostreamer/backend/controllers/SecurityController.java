package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 12:24
 */
@RequestMapping("security")
@Controller
public class SecurityController {
    @RequestMapping(value = "initialize", method = RequestMethod.POST)
    @ResponseBody
    public Object info(@AuthenticationPrincipal AppUserDetails appUserDetails, HttpServletRequest request) {
        return VideostreamerUtils.makeUserInfo(appUserDetails, request);
    }
}
