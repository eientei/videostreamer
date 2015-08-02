package org.eientei.video.backend.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 11:32
 */
public class ChatImageDTO {
    private String image;

    public ChatImageDTO() {
    }

    public ChatImageDTO(String image) {

        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
