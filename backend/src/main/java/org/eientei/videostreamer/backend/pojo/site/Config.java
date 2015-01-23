package org.eientei.videostreamer.backend.pojo.site;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:09
 */
public class Config {
    private String rtmpBase;
    private String captchaPublic;
    private int maxStreams;

    public String getRtmpBase() {
        return rtmpBase;
    }

    public void setRtmpBase(String rtmpBase) {
        this.rtmpBase = rtmpBase;
    }

    public String getCaptchaPublic() {
        return captchaPublic;
    }

    public void setCaptchaPublic(String captchaPublic) {
        this.captchaPublic = captchaPublic;
    }

    public int getMaxStreams() {
        return maxStreams;
    }

    public void setMaxStreams(int maxStreams) {
        this.maxStreams = maxStreams;
    }
}
