package org.eientei.videostreamer.html5;

import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Alexander Tumin on 2016-09-29
 */
@Component
public class Html5RtmpClient implements RtmpClient {
    private final Html5Server html5Server;
    private Html5Stream stream;

    @Autowired
    public Html5RtmpClient(Html5Server html5Server) {
        this.html5Server = html5Server;
    }

    @Override
    public void accept(RtmpMessage message) {
        //stream.update(message);
    }

    @Override
    public void init(String streamName) {
        stream = html5Server.getStream(streamName);
    }
}
