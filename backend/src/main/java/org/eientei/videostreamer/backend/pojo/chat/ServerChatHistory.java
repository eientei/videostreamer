package org.eientei.videostreamer.backend.pojo.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:33
 */
public class ServerChatHistory {
    private List<ServerChatMessageItem> items = new ArrayList<ServerChatMessageItem>();
    private boolean hasMore;

    public List<ServerChatMessageItem> getItems() {
        return items;
    }

    public void setItems(List<ServerChatMessageItem> items) {
        this.items = items;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
