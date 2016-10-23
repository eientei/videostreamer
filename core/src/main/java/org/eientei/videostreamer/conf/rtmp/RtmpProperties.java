package org.eientei.videostreamer.conf.rtmp;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpProperties {
    private boolean enabled = true;

    private String host = "0.0.0.0";

    @Min(0)
    @Max(65536)
    private int port = 1935;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
