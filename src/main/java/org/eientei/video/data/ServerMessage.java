package org.eientei.video.data;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:35
 */
public class ServerMessage {
    private MessageType type;
    private Object data;

    public ServerMessage(MessageType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
