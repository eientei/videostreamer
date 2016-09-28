package org.eientei.videostreamer.config.mail;

import javax.validation.constraints.NotNull;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class MailProperties {
    @NotNull
    private String smtpd;

    @NotNull
    private String account;

    @NotNull
    private String auth;

    public String getSmtpd() {
        return smtpd;
    }

    public void setSmtpd(String smtpd) {
        this.smtpd = smtpd;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
}
