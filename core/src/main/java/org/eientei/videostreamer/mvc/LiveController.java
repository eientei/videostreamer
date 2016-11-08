package org.eientei.videostreamer.mvc;

import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.core.StreamContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
@Controller
@RequestMapping("live")
public class LiveController {
    private final GlobalContext globalContext;

    @Autowired
    public LiveController(GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @ResponseBody
    @RequestMapping(value = "{name}.mp4")
    public Object stream(HttpServletResponse res, @PathVariable String name) {
        final StreamContext stream = globalContext.stream(name);
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }
        res.addHeader("Content-Type", "video/mp4");
        return new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                EmbeddedChannel embed = new EmbeddedChannel(DefaultChannelId.newInstance(), new ChunkedOutputHandler(outputStream));
                stream.addRemuxSubscriber(embed);
                while (embed.isOpen()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
}
