package org.eientei.videostreamer.backend.pojo.user;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:31
 */
public class UpdateCredentials {
    private String originalPassword;
    private String password;

    public String getOriginalPassword() {
        return originalPassword;
    }

    public void setOriginalPassword(String originalPassword) {
        this.originalPassword = originalPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
