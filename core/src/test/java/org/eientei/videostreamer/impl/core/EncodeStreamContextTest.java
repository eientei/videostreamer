package org.eientei.videostreamer.impl.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.eientei.videostreamer.impl.handlers.RtmpMessageInboundBroadcastHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class EncodeStreamContextTest {
    private Channel channel = new EmbeddedChannel();
    private Channel anotherChannel = new EmbeddedChannel();

    @Test
    public void testName() {
        Assert.assertEquals("name", new StreamContext(new GlobalContext(), "name", new DefaultEventExecutor()).getName());
    }

    @Test
    public void testPublisher() {
        StreamContext stream = new StreamContext(new GlobalContext(), "name", new DefaultEventExecutor());
        Assert.assertEquals(null, stream.getPublisher());
        stream.setPublisher(null);
        Assert.assertEquals(null, stream.getPublisher());
        stream.setPublisher(channel);
        Assert.assertEquals(channel, stream.getPublisher());
        stream.setPublisher(anotherChannel);
        Assert.assertEquals(channel, stream.getPublisher());
        stream.setPublisher(null);
        Assert.assertEquals(null, stream.getPublisher());
    }

    @Test
    public void propMeta() {
        StreamContext context = new StreamContext(new GlobalContext(), "test", new DefaultEventExecutor());
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageInboundBroadcastHandler(context));
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_META, 1), Unpooled.buffer()));

    }

    @Test
    public void propVideo() {
        StreamContext context = new StreamContext(new GlobalContext(), "test", new DefaultEventExecutor());
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageInboundBroadcastHandler(context));
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer()));
    }

    @Test
    public void propAudio() {
        StreamContext context = new StreamContext(new GlobalContext(), "test", new DefaultEventExecutor());
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageInboundBroadcastHandler(context));
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AUDIO, 1), Unpooled.buffer()));
    }
}
