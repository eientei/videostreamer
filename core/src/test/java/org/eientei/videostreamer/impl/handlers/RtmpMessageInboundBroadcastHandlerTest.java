package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.StreamContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class RtmpMessageInboundBroadcastHandlerTest {
    @Test
    public void testRtmpMessageBroadcaster() {
        StreamContext context = new StreamContext(new GlobalContext(), "test", new DefaultEventExecutor());

        EmbeddedChannel subscriber1 = new EmbeddedChannel(DefaultChannelId.newInstance());
        context.addRtmpSubscriber(subscriber1);

        EmbeddedChannel subscriber2 = new EmbeddedChannel(DefaultChannelId.newInstance());
        context.addRtmpSubscriber(subscriber2);

        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageInboundBroadcastHandler(context));

        Message message = new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeInbound(message);

        Message msg1 = subscriber1.readOutbound();
        Message msg2 = subscriber2.readOutbound();

        context.release();

        Assert.assertSame(msg1, msg2);
        Assert.assertEquals(2, message.refCnt());
        msg1.release();
        msg2.release();
        Assert.assertEquals(0, message.refCnt());
    }
}