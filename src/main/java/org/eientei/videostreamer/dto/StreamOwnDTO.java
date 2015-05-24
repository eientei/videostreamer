package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.orm.entity.Stream;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 09:49
 */
public class StreamOwnDTO extends StreamDTO {
    private String token;
    private String remote;
    private boolean restricted;

    public StreamOwnDTO(Stream stream) {
        super(stream, null);
        this.token = stream.getToken();
        this.remote = stream.getRemote();
        this.restricted = stream.isRestricted();
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
