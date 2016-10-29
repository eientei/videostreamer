package org.eientei.videostreamer;

import org.eientei.videostreamer.conf.Config;
import org.eientei.videostreamer.server.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
@SpringBootApplication(scanBasePackageClasses = {
        Config.class,
        Server.class
})
public class Launcher {
    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }
}
