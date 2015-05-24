package org.eientei.videostreamer.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-11
 * Time: 13:48
 */
public class ChatTopicDTO {
    private String topic;

    public ChatTopicDTO() {
    }

    public ChatTopicDTO(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
