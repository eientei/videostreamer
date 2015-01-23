package org.eientei.videostreamer.backend.pojo.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:33
 */
public class ServerChatTyping {
    private List<String> items = new ArrayList<String>();

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
