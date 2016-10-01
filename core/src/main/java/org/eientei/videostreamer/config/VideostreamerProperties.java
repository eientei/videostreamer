package org.eientei.videostreamer.config;

import org.eientei.videostreamer.config.crypto.CryptoProperties;
import org.eientei.videostreamer.config.htlm5.Html5Properties;
import org.eientei.videostreamer.config.mail.MailProperties;
import org.eientei.videostreamer.config.mvc.MvcProperties;
import org.eientei.videostreamer.config.rtmp.RtmpProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@ConfigurationProperties("videostreamer")
public class VideostreamerProperties {
    @Valid
    private CryptoProperties crypto;

    @Valid
    private MailProperties mail;

    @Valid
    private MvcProperties mvc;

    @Valid
    @NotNull
    private Html5Properties html5;

    @Valid
    @NotNull
    private RtmpProperties rtmp;

    public CryptoProperties getCrypto() {
        return crypto;
    }

    public void setCrypto(CryptoProperties crypto) {
        this.crypto = crypto;
    }

    public MailProperties getMail() {
        return mail;
    }

    public void setMail(MailProperties mail) {
        this.mail = mail;
    }

    public MvcProperties getMvc() {
        return mvc;
    }

    public void setMvc(MvcProperties mvc) {
        this.mvc = mvc;
    }

    public Html5Properties getHtml5() {
        return html5;
    }

    public void setHtml5(Html5Properties html5) {
        this.html5 = html5;
    }

    public RtmpProperties getRtmp() {
        return rtmp;
    }

    public void setRtmp(RtmpProperties rtmp) {
        this.rtmp = rtmp;
    }
}
