package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.dto.parser.MessageNode;
import org.eientei.videostreamer.orm.entity.Message;

import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-11
 * Time: 13:45
 */
public class ChatMessageDTO {
    private MessageNode root;
    private String text;
    private Long id;
    private String hash;
    private String remoteHash;
    private Date posted;
    private String username;
    private Boolean admin;
    private Boolean owner;


    public ChatMessageDTO() {
    }

    public ChatMessageDTO(Message message) {
        this.id = message.getId();
        this.hash = Util.hash(message.getAuthor(), message.getRemote());
        this.root = Util.parseMessage(message.getMessage());
        this.remoteHash = Util.hash(message.getRemote());
        this.posted = message.getPosted();
        this.username = message.getAuthor().getName();
        this.admin = message.isAdmin();
        this.owner = message.isAuthor();
    }

    public ChatMessageDTO(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getHash() {
        return hash;
    }

    public String getRemoteHash() {
        return remoteHash;
    }

    public Date getPosted() {
        return posted;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public Boolean getOwner() {
        return owner;
    }

    public MessageNode getRoot() {
        return root;
    }
}
