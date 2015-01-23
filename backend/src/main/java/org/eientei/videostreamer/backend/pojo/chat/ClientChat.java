package org.eientei.videostreamer.backend.pojo.chat;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:11
 */
public class ClientChat {
    private ChatMessageType type;
    private Map data;

    public ChatMessageType getType() {
        return type;
    }

    public void setType(ChatMessageType type) {
        this.type = type;
    }

    public Map getData() {
        return data;
    }

    public void setData(Map data) {
        this.data = data;
    }

    public <T> T adaptTo(Class<T> clazz) {
        T instance = BeanUtils.instantiate(clazz);
        DataBinder binder = new DataBinder(instance);
        binder.bind(new MutablePropertyValues(data));
        return instance;
    }
}
