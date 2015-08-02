package org.eientei.video.backend.dto;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.validation.DataBinder;

import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-11
 * Time: 13:13
 */
public class ChatMessageDispatcher {
    public enum Type {
        CONNECT(ChatConnectDTO.class),
        ONLINE(ChatOnlineDTO.class),
        TYPING(ChatTypingDTO.class),
        KEYPRESS(ChatKeypressDTO.class),
        MESSAGE(ChatMessageDTO.class),
        HISTORY(ChatHistoryDTO.class),
        PREVIEW(ChatPreviewDTO.class),
        HISTORYREQUEST(ChatHistoryRequestDTO.class),
        PREVIEWREQUEST(ChatPreviewRequestDTO.class),
        TOPIC(ChatTopicDTO.class),
        IMAGE(ChatImageDTO.class),
        MIGRATE(ChatMigrateDTO.class),
        INFO(ChatInfoDTO.class);

        private Class<?> clazz;

        Type(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> getClazz() {
            return clazz;
        }
    }

    private Type type;
    private Map<String, Object> data;

    public ChatMessageDispatcher() {
    }

    @SuppressWarnings("unchecked")
    public ChatMessageDispatcher(Object object) {
        for (Type t: Type.values()) {
            if (t.getClazz().isAssignableFrom(object.getClass())) {
                type = t;
                break;
            }
        }
        if (type == null) {
            throw new IllegalArgumentException("Unknown message type");
        }

        data = (Map<String, Object>) BeanMap.create(object);
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> T dispatch() {
        T instance = (T) BeanUtils.instantiate(type.getClazz());
        DataBinder dataBinder = new DataBinder(instance);
        dataBinder.bind(new MutablePropertyValues(data));
        return instance;
    }
}
