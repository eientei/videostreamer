package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.controller.model.ChatClient;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-08
 * Time: 18:54
 */
public class ChatClientDTO {
    private String hash;
    private String name;

    public ChatClientDTO(ChatClient client, long time) {
        this.hash = Util.hash(client.getUser().getEntity(), client.getSession().getAttributes().get("remoteips").toString());
        this.name = client.getUser().getUsername();
    }

    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }
}
