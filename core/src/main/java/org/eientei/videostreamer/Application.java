package org.eientei.videostreamer;

import org.eientei.videostreamer.config.Config;
import org.eientei.videostreamer.rtmp.RtmpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@SpringBootApplication(scanBasePackageClasses = {
        Config.class,
        RtmpServer.class
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
