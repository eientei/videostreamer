package org.eientei.video.form;

import org.eientei.video.orm.entity.Stream;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-25
 * Time: 18:25
 */
public class ProfileForm {
    private String action;

    private String passwordOriginal;
    private String password;
    private String passwordRepeat;

    private String email;

    private Stream stream;

    public ProfileForm() {
    }

    public ProfileForm(String email, Stream stream) {
        this.email = email;
        this.stream = stream;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPasswordOriginal() {
        return passwordOriginal;
    }

    public void setPasswordOriginal(String passwordOriginal) {
        this.passwordOriginal = passwordOriginal;
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

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream streams) {
        this.stream = streams;
    }
}
