package org.eientei.videostreamer.html5;

import org.eientei.videostreamer.rtmp.RtmpMessage;

import java.io.IOException;
import java.nio.channels.Pipe;

/**
 * Created by Alexander Tumin on 2016-09-29
 */
public class Html5Stream {
    private Pipe inputPipe;
    private Pipe outputPipe;
    private int width;
    private int height;
    private int fps;

    public Html5Stream(Html5Server html5Server) {
    }

    public void start() {
        try {
            this.inputPipe = Pipe.open();
            this.outputPipe = Pipe.open();
        } catch (IOException e) {
        }
    }

    public void stop() {
    }

    @SuppressWarnings("unchecked")
    public void update(RtmpMessage message) {

    }
}
