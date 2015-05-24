package org.eientei.videostreamer.dto;

import java.util.LinkedList;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-12
 * Time: 08:37
 */
public class ChatHistoryDTO {
    private LinkedList<ChatMessageDTO> history = new LinkedList<ChatMessageDTO>();
    private boolean more;

    public LinkedList<ChatMessageDTO> getHistory() {
        return history;
    }

    public boolean isMore() {
        return more;
    }

    public void setMore(boolean more) {
        this.more = more;
    }
}
