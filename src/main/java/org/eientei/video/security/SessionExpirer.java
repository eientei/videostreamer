package org.eientei.video.security;

import javax.servlet.http.HttpServletRequest;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 12:27
 */
public class SessionExpirer {
    private int interval;

    public void expireSession(HttpServletRequest request) {
        request.getSession().setMaxInactiveInterval(interval);
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
