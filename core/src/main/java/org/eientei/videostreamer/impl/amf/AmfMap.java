package org.eientei.videostreamer.impl.amf;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class AmfMap extends HashMap<String, Object> {
    public AmfMap() {
    }

    public AmfMap(Map<? extends String, ?> m) {
        super(m);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(String key) {
        return (T) get(key);
    }

}
