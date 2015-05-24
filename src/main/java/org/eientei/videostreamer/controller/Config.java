package org.eientei.videostreamer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-08
 * Time: 12:18
 */
@Configuration
@RestController
@RequestMapping("config")
public class Config {

    @Value("${videostreamer.rtmpPrefix:}")
    private String rtmpPrefix;

    @Value("${videostreamer.captcha.secret:}")
    private String captchaPubkey;

    @RequestMapping("rtmp")
    public String rtmp() {
        return '"' + rtmpPrefix + '"';
    }

    @RequestMapping("captcha")
    public String captcha() {
        return '"' + captchaPubkey + '"';
    }
}
