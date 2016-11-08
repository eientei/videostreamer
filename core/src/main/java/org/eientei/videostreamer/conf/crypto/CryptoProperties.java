package org.eientei.videostreamer.conf.crypto;

import javax.validation.constraints.NotNull;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class CryptoProperties {
    @NotNull
    private String salt;

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
