package org.eientei.video.orm.entity;

import org.eientei.video.data.ChatRoom;
import org.eientei.video.orm.util.VideostreamUtils;

import javax.persistence.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-14
 * Time: 12:36
 */
@Entity
@Table(name = "streams", uniqueConstraints = @UniqueConstraint(columnNames = {"app", "name"}))
public class Stream {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(nullable = false)
    private String app;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String remote;

    @Column(nullable = true)
    private String topic;

    @Column(nullable = true)
    private String idleImage;

    @Transient
    private String hash;

    @Transient
    private ChatRoom chatRoom;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getIdleImage() {
        return idleImage;
    }

    public void setIdleImage(String idleImage) {
        this.idleImage = idleImage;
    }

    public String getHash() throws NoSuchAlgorithmException {
        if (hash == null) {
            String hashSource;
            String email = author.getEmail();
            if (email != null && !email.isEmpty()) {
                hashSource = email;
            } else {
                hashSource = remote;
            }
            hash = VideostreamUtils.hashMd5(hashSource);
        }
        return hash;
    }

    public int getUsers() {
        if (chatRoom == null) {
            return 0;
        }
        return chatRoom.getClients();
    }


    public void setChatRoom(ChatRoom room) {
        this.chatRoom = room;
    }
}
