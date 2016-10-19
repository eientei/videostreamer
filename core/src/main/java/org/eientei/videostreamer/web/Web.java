package org.eientei.videostreamer.web;

import org.eientei.videostreamer.rtmp.server.RtmpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Created by Alexander Tumin on 2016-10-18
 */
@Controller
public class Web {
    private final RtmpServer server;

    @Autowired
    public Web(RtmpServer server) {
        this.server = server;
    }

      /*
    @ResponseBody
    @RequestMapping(value = "/live/baka.mp4", produces = "video/mp4")
    public ResponseBodyEmitter video(HttpServletRequest request) {

        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        RtmpStream stream = server.acquireStream("baka");
        final WebsocketClient client = new WebsocketClient(stream, emitter);
        stream.subscribe(client);
        emitter.onCompletion(new Runnable() {
            @Override
            public void run() {
                client.getRtmpStream().cleanup(client);
            }
        });
        emitter.onTimeout(new Runnable() {
            @Override
            public void run() {
                client.getRtmpStream().cleanup(client);
            }
        });
        return emitter;
        //return new ResponseEntity<InputStream>(new PseudoInputStream(client), HttpStatus.OK);
    }
    */
}
