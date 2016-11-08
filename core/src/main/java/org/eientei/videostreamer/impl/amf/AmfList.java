package org.eientei.videostreamer.impl.amf;

import java.util.ArrayList;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class AmfList extends ArrayList<Object> {
    @SuppressWarnings("unchecked")
    public <T> T getAs(int i) {
        return (T) get(i);
    }
}
