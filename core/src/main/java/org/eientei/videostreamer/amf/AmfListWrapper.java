package org.eientei.videostreamer.amf;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class AmfListWrapper {
    private final List<Object> data;

    public AmfListWrapper(List<Object> data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        return (T)data.get(index);
    }
}
