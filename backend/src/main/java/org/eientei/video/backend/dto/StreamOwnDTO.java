package org.eientei.video.backend.dto;

import org.eientei.video.backend.controller.model.ChatRoom;
import org.eientei.video.backend.orm.entity.Stream;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 09:49
 */
public class StreamOwnDTO extends StreamDTO {
    private String token;
    private String remote;
    private boolean restricted;


    public StreamOwnDTO(Stream stream, ChatRoom chatRoom) {
        super(stream, chatRoom);
        this.token = stream.getToken();
        this.remote = stream.getRemote();
        this.restricted = stream.isRestricted();
        this.ownstream = true;
    }

    public StreamOwnDTO(Stream stream) {
        this(stream, null);
    }

    public String getToken() {
        return token;
    }

    public String getRemote() {
        return remote;
    }

    public boolean getRestricted() {
        return restricted;
    }
}
