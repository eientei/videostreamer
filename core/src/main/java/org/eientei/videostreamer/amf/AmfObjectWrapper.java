package org.eientei.videostreamer.amf;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-09-27
 */
public class AmfObjectWrapper extends HashMap<String,Object> {
    public AmfObjectWrapper() {
    }

    public AmfObjectWrapper(Map<String, Object> map) {
        putAll(map);
    }
}
