package org.eientei.video.data;

import org.eientei.video.orm.entity.Message;
import org.eientei.video.orm.util.VideostreamUtils;

import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:33
 */
public class ChatMessage {
    private int id;
    private Date posted;
    private String remote;
    private String author;
    private String message;

    public ChatMessage(Message msg) {
        id = msg.getId();
        posted = msg.getPosted();
        remote = VideostreamUtils.hashMd5(msg.getRemote());
        message = VideostreamUtils.preProcess(msg.getMessage());
        author = VideostreamUtils.determineUserHash(msg.getAuthor(), msg.getRemote());
    }

    public int getId() {
        return id;
    }

    public Date getPosted() {
        return posted;
    }

    public String getRemote() {
        return remote;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }
}
