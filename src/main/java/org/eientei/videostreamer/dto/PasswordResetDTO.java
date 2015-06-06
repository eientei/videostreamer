package org.eientei.videostreamer.dto;

/**
 * User: user
 * Date: 2015-06-06
 * Time: 00:28
 */
public class PasswordResetDTO {
    private String resetkey;
    private String password;

    public String getResetkey() {
        return resetkey;
    }

    public void setResetkey(String resetkey) {
        this.resetkey = resetkey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
