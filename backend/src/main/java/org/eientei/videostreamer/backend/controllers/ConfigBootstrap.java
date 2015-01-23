package org.eientei.videostreamer.backend.controllers;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-21
 * Time: 11:27
 */
@Component
public class ConfigBootstrap {
    private boolean captchaEnabled;
    private String recaptchaPrivate;
    private String recaptchaPublic;
    private String rtmpBase;
    private String videoMailHost;
    private String videoMailPassword;
    private String videoMailFrom;
    private int maxStreams = 5;

    @PostConstruct
    public void postConstruct() {
        try {
            Context context = new InitialContext();
            try {
                captchaEnabled = Boolean.parseBoolean((String) context.lookup("jdbc/VideoCaptcha"));
                recaptchaPrivate = (String) context.lookup("jdbc/VideoReCaptchaPrivate");
                recaptchaPublic = (String) context.lookup("jdbc/VideoReCaptchaPublic");
            } catch (Exception ignore) {
                captchaEnabled = false;
            }
            try {
                rtmpBase = (String) context.lookup("jdbc/VideoRtmpBase");
            } catch (Exception ignore) {
                rtmpBase = "";
            }
            try {
                videoMailHost = (String) context.lookup("jdbc/VideoMailHost");
                videoMailPassword = (String) context.lookup("jdbc/VideoMailPassword");
                videoMailFrom = (String) context.lookup("jdbc/VideoMailFrom");
            } catch (Exception ignore) {

            }
            try {
                maxStreams = Integer.parseInt((String) context.lookup("jdbc/MaxStreams"));
            } catch (Exception ignore) {

            }
        } catch (Exception ignore) {
        }
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public String getRecaptchaPrivate() {
        return recaptchaPrivate;
    }

    public String getRecaptchaPublic() {
        return recaptchaPublic;
    }

    public String getRtmpBase() {
        return rtmpBase;
    }

    public String getVideoMailHost() {
        return videoMailHost;
    }

    public String getVideoMailPassword() {
        return videoMailPassword;
    }

    public String getVideoMailFrom() {
        return videoMailFrom;
    }

    public int getMaxStreams() {
        return maxStreams;
    }
}
