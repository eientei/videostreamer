package org.eientei.videostreamer.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 08:09
 */
public class UserDTO {
    private String username;
    private String hashicon;
    private String email;

    public UserDTO(String username, String hashicon, String email) {
        this.username = username;
        this.hashicon = hashicon;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getHashicon() {
        return hashicon;
    }

    public String getEmail() {
        return email;
    }
}
