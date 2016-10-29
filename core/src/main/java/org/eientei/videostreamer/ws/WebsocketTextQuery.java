package org.eientei.videostreamer.ws;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class WebsocketTextQuery {
    private final String action;
    private final String params;

    public WebsocketTextQuery(String action, String params) {
        this.action = action;
        this.params = params;
    }

    public String getAction() {
        return action;
    }

    public String getParams() {
        return params;
    }
}
