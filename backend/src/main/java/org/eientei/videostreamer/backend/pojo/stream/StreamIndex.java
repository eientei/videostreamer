package org.eientei.videostreamer.backend.pojo.stream;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:13
 */
public class StreamIndex {
    private List<StreamIndexItem> items = new ArrayList<StreamIndexItem>();

    public List<StreamIndexItem> getItems() {
        return items;
    }

    public void setItems(List<StreamIndexItem> items) {
        this.items = items;
    }
}
