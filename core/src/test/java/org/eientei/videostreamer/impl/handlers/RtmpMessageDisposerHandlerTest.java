package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class RtmpMessageDisposerHandlerTest {
    @Test
    public void testMessageDisposer() {
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageDisposerHandler());
        Message message = new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeInbound(message);
        Assert.assertEquals(0, message.refCnt());
    }
}