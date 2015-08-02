package org.eientei.video.backend.dto;

import org.eientei.video.backend.controller.validation.ReCaptcha;

/**
 * User: user
 * Date: 2015-06-05
 * Time: 23:42
 */
public class PasswordResetRequestDTO {
    private String username;
    private String email;

    @ReCaptcha
    private String captcha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
