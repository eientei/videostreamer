package org.eientei.videostreamer.backend.pojo.chat;

import java.util.Set;
import java.util.TreeSet;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:32
 */
public class ServerChatOnline {
    private Set<ServerChatOnlineItem> items = new TreeSet<ServerChatOnlineItem>();

    public Set<ServerChatOnlineItem> getItems() {
        return items;
    }

    public void setItems(Set<ServerChatOnlineItem> items) {
        this.items = items;
    }
}
