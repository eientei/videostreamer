package org.eientei.videostreamer.backend.pojo.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:13
 */
public enum ChatMessageType {
    CONNECT(1),
    MESSAGE(2),
    HISTORY(3),
    TYPING(4),
    TOPIC(5),
    ONLINE(6);

    private int id;

    ChatMessageType(int i) {
        this.id = i;
    }

    public int getId() {
        return id;
    }

    @JsonCreator
    public static ChatMessageType forValue(String value) {
        int id = Integer.parseInt(value);
        for (ChatMessageType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public int toValue() {
        return id;
    }
}
