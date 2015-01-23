package org.eientei.videostreamer.backend.pojo.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:31
 */
public class ServerChatMessage {
    private List<ServerChatMessageItem> items = new ArrayList<ServerChatMessageItem>();

    public List<ServerChatMessageItem> getItems() {
        return items;
    }

    public void setItems(List<ServerChatMessageItem> items) {
        this.items = items;
    }
}
