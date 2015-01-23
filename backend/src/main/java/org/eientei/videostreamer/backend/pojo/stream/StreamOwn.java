package org.eientei.videostreamer.backend.pojo.stream;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:21
 */
public class StreamOwn {
    private List<StreamOwnItem> items = new ArrayList<StreamOwnItem>();

    public List<StreamOwnItem> getItems() {
        return items;
    }

    public void setItems(List<StreamOwnItem> items) {
        this.items = items;
    }
}
