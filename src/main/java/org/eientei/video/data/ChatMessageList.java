package org.eientei.video.data;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:35
 */
public class ChatMessageList {
    private List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
    private boolean hasMore;

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
