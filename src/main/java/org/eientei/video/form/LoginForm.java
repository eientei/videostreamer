package org.eientei.video.form;

import org.hibernate.validator.constraints.NotBlank;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 09:36
 */
public class LoginForm {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
