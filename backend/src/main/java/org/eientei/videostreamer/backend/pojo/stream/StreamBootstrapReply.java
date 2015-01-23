package org.eientei.videostreamer.backend.pojo.stream;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:20
 */
public class StreamBootstrapReply {
    private boolean ok;
    private long id;
    private String idleImage;
    private String topic;
    private boolean own;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIdleImage() {
        return idleImage;
    }

    public void setIdleImage(String idleImage) {
        this.idleImage = idleImage;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isOwn() {
        return own;
    }

    public void setOwn(boolean own) {
        this.own = own;
    }
}
