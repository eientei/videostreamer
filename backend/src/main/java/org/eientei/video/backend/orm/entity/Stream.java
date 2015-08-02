package org.eientei.video.backend.orm.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 02:09
 */
@Entity
@Table(name = "streams", uniqueConstraints = @UniqueConstraint(columnNames = {"app", "name"}))
public class Stream {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "author")
    private User author;

    @Column(nullable = false)
    private String app;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String remote;

    @Column(nullable = true)
    private String topic;

    @Column(name = "idleimage", nullable = true)
    private String idleImage;

    @Column(nullable = true)
    private Date since;

    @Column(nullable = false)
    private Boolean restricted = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }

    public Boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(Boolean restricted) {
        this.restricted = restricted;
    }
}
