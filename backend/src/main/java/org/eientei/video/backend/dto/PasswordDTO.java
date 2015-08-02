package org.eientei.video.backend.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 13:23
 */
public class PasswordDTO {
    private String current;
    private String desired;

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getDesired() {
        return desired;
    }

    public void setDesired(String desired) {
        this.desired = desired;
    }
}
