package org.eientei.video.backend.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-28
 * Time: 09:44
 */
public class ConfigDTO {
    private final String rtmpPrefix;
    private final String captchaPubkey;

    public ConfigDTO(String rtmpPrefix, String captchaPubkey) {
        this.rtmpPrefix = rtmpPrefix;
        this.captchaPubkey = captchaPubkey;
    }

    public String getRtmpPrefix() {
        return rtmpPrefix;
    }

    public String getCaptchaPubkey() {
        return captchaPubkey;
    }
}
