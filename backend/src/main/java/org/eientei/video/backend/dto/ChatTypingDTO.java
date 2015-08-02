package org.eientei.video.backend.dto;


import org.eientei.video.backend.controller.model.ChatClient;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 20:10
 */
public class ChatTypingDTO {
    List<String> typers = new ArrayList<String>();

    public ChatTypingDTO(List<ChatClient> clients) {
        for (ChatClient client : clients) {
            typers.add(Util.hash(client.getUser().getEntity(), client.getSession().getAttributes().get("remoteips").toString()));
        }
    }

    public List<String> getTypers() {
        return typers;
    }
}
