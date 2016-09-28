package org.eientei.videostreamer.config.mvc;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class MvcProperties {
    @Valid
    private ReCaptcha reCaptcha;

    public ReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(ReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public static class ReCaptcha {
        @NotNull
        private boolean enabled;

        @NotNull
        private String privateToken;

        @NotNull
        private String publicToken;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPrivateToken() {
            return privateToken;
        }

        public void setPrivateToken(String privateToken) {
            this.privateToken = privateToken;
        }

        public String getPublicToken() {
            return publicToken;
        }

        public void setPublicToken(String publicToken) {
            this.publicToken = publicToken;
        }
    }
}
