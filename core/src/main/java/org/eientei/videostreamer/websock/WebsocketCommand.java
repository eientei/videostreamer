package org.eientei.videostreamer.websock;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class WebsocketCommand {
    private String stream;
    private String action;

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
