package org.eientei.videostreamer.amf;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-09-27
 */
public class AmfObjectWrapper<K, V> extends HashMap<K,V> {
    public AmfObjectWrapper(Map<K, V> map) {
        putAll(map);
    }
}
