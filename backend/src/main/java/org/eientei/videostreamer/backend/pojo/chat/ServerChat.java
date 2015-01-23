package org.eientei.videostreamer.backend.pojo.chat;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:34
 */
public class ServerChat {
    private ChatMessageType type;
    private Object data;

    public ChatMessageType getType() {
        return type;
    }

    public void setType(ChatMessageType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
