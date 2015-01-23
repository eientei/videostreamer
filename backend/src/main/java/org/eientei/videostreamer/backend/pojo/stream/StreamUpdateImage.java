package org.eientei.videostreamer.backend.pojo.stream;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:18
 */
public class StreamUpdateImage {
    private long id;
    private String image;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
