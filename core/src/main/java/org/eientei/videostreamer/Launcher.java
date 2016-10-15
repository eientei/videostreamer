package org.eientei.videostreamer;

import org.eientei.videostreamer.config.Config;
import org.eientei.videostreamer.rtmp.server.RtmpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
@SpringBootApplication(scanBasePackageClasses = {
        Config.class,
        RtmpServer.class
})
public class Launcher {
    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }
}
