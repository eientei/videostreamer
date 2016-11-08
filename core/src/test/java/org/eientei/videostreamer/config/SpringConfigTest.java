package org.eientei.videostreamer.config;

import org.eientei.videostreamer.conf.VideostreamerProperties;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
/*
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConfiguration
@TestPropertySource(properties = {
        "videostreamer.crypto.salt=blurp",
        "videostreamer.mail.smtpd=0.0.0.0",
        "videostreamer.mail.account=admin@0.0.0.0",
        "videostreamer.mail.auth=none",
        "videostreamer.mvc.reCaptcha.privateToken=someblob",
        "videostreamer.mvc.reCaptcha.publicToken=someblob",
        "videostreamer.mvc.reCaptcha.enabled=true",
        "videostreamer.mvc.domain=0.0.0.0",
        "videostreamer.rtmp.enabled=true",
        "videostreamer.rtmp.host=0.0.0.0",
        "videostreamer.rtmp.port=1935"
})*/
public class SpringConfigTest {
    @Autowired
    private VideostreamerProperties properties;

    //@Test
    public void test() {
        Assert.assertEquals("blurp", properties.getCrypto().getSalt());
        Assert.assertEquals("0.0.0.0", properties.getMail().getSmtpd());
        Assert.assertEquals("admin@0.0.0.0", properties.getMail().getAccount());
        Assert.assertEquals("none", properties.getMail().getAuth());
        Assert.assertEquals("someblob", properties.getMvc().getReCaptcha().getPrivateToken());
        Assert.assertEquals("someblob", properties.getMvc().getReCaptcha().getPublicToken());
        Assert.assertEquals(true, properties.getMvc().getReCaptcha().isEnabled());
        Assert.assertEquals("0.0.0.0", properties.getMvc().getDomain());
        Assert.assertEquals(true, properties.getRtmp().isEnabled());
        Assert.assertEquals("0.0.0.0", properties.getRtmp().getHost());
        Assert.assertEquals(1935, properties.getRtmp().getPort());
    }
}
