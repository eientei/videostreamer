package org.eientei.videostreamer.backend.pojo.user;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:25
 */
public class Signup {
    private String captchaChallenge;
    private String CaptchaResponse;
    private String name;
    private String password;
    private String email;

    public String getCaptchaChallenge() {
        return captchaChallenge;
    }

    public void setCaptchaChallenge(String captchaChallenge) {
        this.captchaChallenge = captchaChallenge;
    }

    public String getCaptchaResponse() {
        return CaptchaResponse;
    }

    public void setCaptchaResponse(String captchaResponse) {
        CaptchaResponse = captchaResponse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
