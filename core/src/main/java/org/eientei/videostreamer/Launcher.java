package org.eientei.videostreamer;

import org.eientei.videostreamer.conf.Config;
import org.eientei.videostreamer.mp4.Mp4Server;
import org.eientei.videostreamer.rtmp.RtmpServer;
import org.eientei.videostreamer.ws.WebsocketCommHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
@SpringBootApplication(scanBasePackageClasses = {
        Config.class,
        RtmpServer.class,
        Mp4Server.class,
        WebsocketCommHandler.class
})
public class Launcher {
    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }
}
