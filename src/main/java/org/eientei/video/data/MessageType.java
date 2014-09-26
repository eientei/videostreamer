package org.eientei.video.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-19
 * Time: 01:01
 */
public enum  MessageType {
    SUCCESS("success"),
    MESSAGE("message"),
    UPDATES("updates"),
    HISTORY("history"),
    PREVIEW("preview"),
    ONLINES("onlines"),
    TYPOING("typoing");

    private static final Map<String, MessageType> registry = new ConcurrentHashMap<String, MessageType>() {{
        for (MessageType messageType : MessageType.values()) {
            put(messageType.getValue(), messageType);
        }
    }};

    @JsonCreator
    public static MessageType dispatch(String value) {
        return registry.get(value);
    }

    private String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
