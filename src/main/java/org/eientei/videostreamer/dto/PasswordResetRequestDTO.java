package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.controller.validation.ReCaptcha;

/**
 * User: user
 * Date: 2015-06-05
 * Time: 23:42
 */
public class PasswordResetRequestDTO {
    private String name;
    private String email;

    @ReCaptcha
    private String captcha;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
