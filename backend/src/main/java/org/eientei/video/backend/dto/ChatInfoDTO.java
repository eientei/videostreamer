package org.eientei.video.backend.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 14:36
 */
public class ChatInfoDTO {
    private boolean owner;
    private String topic;
    private String image;

    public ChatInfoDTO(boolean owner, String topic, String image) {
        this.owner = owner;
        this.topic = topic;
        this.image = image;
    }

    public boolean isOwner() {
        return owner;
    }

    public String getTopic() {
        return topic;
    }

    public String getImage() {
        return image;
    }
}
