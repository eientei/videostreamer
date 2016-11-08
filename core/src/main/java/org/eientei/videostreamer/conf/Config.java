package org.eientei.videostreamer.conf;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@Configuration
@EnableConfigurationProperties({
        VideostreamerProperties.class
})
public class Config {
    public Config() {
    }
}
