package org.eientei.video.data;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:34
 */
public class ClientHandshake {
    private String app;
    private String stream;
    private boolean oldUser;
    private int oldMessageId;
    private int newMessageId;

    public String getApp() {
        return app;
    }

    public String getStream() {
        return stream;
    }

    public boolean isOldUser() {
        return oldUser;
    }

    public int getOldMessageId() {
        return oldMessageId;
    }

    public int getNewMessageId() {
        return newMessageId;
    }
}
