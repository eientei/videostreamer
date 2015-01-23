package org.eientei.videostreamer.backend.pojo.site;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:14
 */
public class UserInfo {
    private long id;
    private String name;
    private String email;
    private String hash;
    private boolean authed;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }
}
