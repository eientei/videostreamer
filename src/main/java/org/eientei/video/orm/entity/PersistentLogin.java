package org.eientei.video.orm.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 08:59
 */
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {
    @Id
    @Column(nullable = false, length = 64)
    private String series;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false, name = "last_used")
    private Date timestamp;

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
