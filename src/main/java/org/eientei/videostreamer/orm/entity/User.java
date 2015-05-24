package org.eientei.videostreamer.orm.entity;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 02:07
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = true)
    private String passwordhash;

    @Column(nullable = true)
    private String email;

    @Column(nullable = false)
    private Date joined = new Date();

    @OneToMany(mappedBy = "author", fetch = FetchType.EAGER)
    private Set<Stream> streams = new HashSet<Stream>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordhash() {
        return passwordhash;
    }

    public void setPasswordhash(String passwordhash) {
        this.passwordhash = passwordhash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getJoined() {
        return joined;
    }

    public void setJoined(Date joined) {
        this.joined = joined;
    }

    public Set<Stream> getStreams() {
        return streams;
    }

    public void setStreams(Set<Stream> streams) {
        this.streams = streams;
    }
}
