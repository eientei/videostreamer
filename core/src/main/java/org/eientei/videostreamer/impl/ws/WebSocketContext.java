package org.eientei.videostreamer.impl.ws;

import org.eientei.videostreamer.impl.core.GlobalContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class WebSocketContext {
    private final GlobalContext globalContext;
    private final WebSocketSession session;

    public WebSocketContext(GlobalContext globalContext, WebSocketSession session) {
        this.globalContext = globalContext;
        this.session = session;
    }

    public void release() {

    }

    public void accept(TextMessage message) {

    }
}
