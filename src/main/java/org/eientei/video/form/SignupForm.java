package org.eientei.video.form;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-14
 * Time: 22:21
 */
public class SignupForm {
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[0-9a-zA-Z_.-]*$")
    private String username;

    @Size(min = 3, max = 64)
    private String password;

    private String passwordRepeat;

    private String email;

    private String captcha;

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

    public String getPasswordRepeat() {
        return passwordRepeat;
    }

    public void setPasswordRepeat(String passwordRepeat) {
        this.passwordRepeat = passwordRepeat;
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
