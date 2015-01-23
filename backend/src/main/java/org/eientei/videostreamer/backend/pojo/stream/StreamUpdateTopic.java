package org.eientei.videostreamer.backend.pojo.stream;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:17
 */
public class StreamUpdateTopic {
    private long id;
    private String topic;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
