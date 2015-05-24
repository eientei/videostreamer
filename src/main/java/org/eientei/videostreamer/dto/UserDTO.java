package org.eientei.videostreamer.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 08:09
 */
public class UserDTO {
    private String username;
    private String hashicon;

    public UserDTO(String username, String hashicon) {
        this.username = username;
        this.hashicon = hashicon;
    }

    public String getUsername() {
        return username;
    }

    public String getHashicon() {
        return hashicon;
    }
}
