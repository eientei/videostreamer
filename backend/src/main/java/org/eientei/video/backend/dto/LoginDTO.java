package org.eientei.video.backend.dto;

import org.eientei.video.backend.controller.validation.ReCaptcha;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-29
 * Time: 21:26
 */
public class LoginDTO {
    @NotEmpty
    @Size(min = 3, max = 30)
    private String username;

    @NotEmpty
    @Size(min = 3, max = 30)
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
