package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.pojo.site.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-21
 * Time: 11:28
 */
@RequestMapping("config")
@Controller
public class ConfigController {
    @Autowired
    private ConfigBootstrap config;

    @RequestMapping("initialize")
    @ResponseBody
    public Object app() {
        Config configData = new Config();
        configData.setRtmpBase(config.getRtmpBase());
        configData.setCaptchaPublic(config.getRecaptchaPublic());
        configData.setMaxStreams(config.getMaxStreams());
        return configData;
    }
}
