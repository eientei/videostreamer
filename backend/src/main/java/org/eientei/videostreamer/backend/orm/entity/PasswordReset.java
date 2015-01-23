package org.eientei.videostreamer.backend.orm.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2014-10-28
 * Time: 12:50
 */
@Entity
@Table(name = "password_reset")
public class PasswordReset {
    @Id
    @Column(nullable = false)
    private String resetkey;

    @Column(nullable = false)
    private Date created = new Date();

    @OneToOne
    @JoinColumn(name = "user")
    private User user;

    public PasswordReset() {
    }

    public PasswordReset(User user) {
        this.user = user;
    }

    public String getResetkey() {
        return resetkey;
    }

    public void setResetkey(String resetkey) {
        this.resetkey = resetkey;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
