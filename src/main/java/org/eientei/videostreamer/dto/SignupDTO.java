package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.controller.validation.ReCaptcha;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 08:47
 */
public class SignupDTO {
    @NotEmpty
    @Size(min = 3, max = 30)
    private String username;

    @NotEmpty
    @Size(min = 3, max = 30)
    private String password;

    @Size(max = 250)
    private String email;

    @ReCaptcha
    @NotEmpty
    private String captcha;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getCaptcha() {
        return captcha;
    }
}
