package org.eientei.videostreamer.mp4.util;

import com.google.common.collect.Maps;
import org.eientei.videostreamer.mp4.box.Box;

import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-09
 */
public class BoxContext {
    private Map<Box.Type, Object> map = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public <T> T get(Box.Type type) {
        T obj = (T) map.get(type);
        if (obj == null) {
            return (T) type.getVal();
        }
        return obj;
    }

    public <T> void put(Box.Type type, T value) {
        map.put(type, value);
    }
}
