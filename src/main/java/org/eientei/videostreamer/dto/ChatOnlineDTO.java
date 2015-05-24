package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.controller.model.ChatClient;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 19:56
 */
public class ChatOnlineDTO {
    private Set<ChatClientDTO> online = new TreeSet<ChatClientDTO>(new Comparator<ChatClientDTO>() {
        @Override
        public int compare(ChatClientDTO o1, ChatClientDTO o2) {
            return o1.getHash().compareTo(o2.getHash());
        }
    });
    private ChatClientDTO owner;

    public ChatOnlineDTO(List<ChatClient> clients) {
        long time = System.currentTimeMillis();
        for (ChatClient client : clients) {
            if (client.isOwner()) {
                owner = new ChatClientDTO(client, time);
            } else {
                online.add(new ChatClientDTO(client, time));
            }
        }
    }

    public Set<ChatClientDTO> getOnline() {
        return online;
    }

    public ChatClientDTO getOwner() {
        return owner;
    }
}
