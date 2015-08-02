package org.eientei.video.backend.controller;

import org.eientei.video.backend.dto.ConfigDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-27
 * Time: 22:06
 */
@Configuration
@RestController
@RequestMapping("config")
public class Config {
    @Value("${videostreamer.rtmpPrefix:}")
    private String rtmpPrefix;

    @Value("${videostreamer.captcha.public:}")
    private String captchaPubkey;

    private ConfigDTO config;

    @PostConstruct
    public void postConstruct() {
        config = new ConfigDTO(rtmpPrefix, captchaPubkey);
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ConfigDTO config() {
        return config;
    }
}
