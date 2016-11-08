package org.eientei.videostreamer.impl.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class StreamContextTest {
    @Test
    public void testStreamContextInit() {
        StreamContext context = new StreamContext("test", new DefaultEventExecutor());
        context.publish(new Message(new Header(2, 0, Message.Type.AMF0_META, 1), Unpooled.buffer()));
        context.publish(new Message(new Header(2, 0, Message.Type.AUDIO, 1), Unpooled.buffer()));
        context.publish(new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer()));
        context.release();
    }

    @Test
    public void testStreamContextSubscriber() {
        StreamContext context = new StreamContext("test", new DefaultEventExecutor());
        EmbeddedChannel channelBefore = new EmbeddedChannel();
        context.addRtmpSubscriber(channelBefore);

        context.publish(new Message(new Header(2, 0, Message.Type.AMF0_META, 1), Unpooled.buffer()));
        Message outmetad = channelBefore.readOutbound();
        Assert.assertEquals(Message.Type.AMF0_META, outmetad.getHeader().getType());
        context.publish(new Message(new Header(2, 0, Message.Type.AUDIO, 1), Unpooled.buffer()));
        Message outaudio = channelBefore.readOutbound();
        Assert.assertEquals(Message.Type.AUDIO, outaudio.getHeader().getType());
        context.publish(new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer()));
        Message outvideo = channelBefore.readOutbound();
        Assert.assertEquals(Message.Type.VIDEO, outvideo.getHeader().getType());

        Assert.assertEquals(null, channelBefore.readOutbound());

        EmbeddedChannel channel = new EmbeddedChannel();
        context.addRtmpSubscriber(channel);
        Message metad = channel.readOutbound();
        Assert.assertEquals(Message.Type.AMF0_META, metad.getHeader().getType());
        Message audio = channel.readOutbound();
        Assert.assertEquals(Message.Type.AUDIO, audio.getHeader().getType());
        Message video = channel.readOutbound();
        Assert.assertEquals(Message.Type.VIDEO, video.getHeader().getType());

        context.publish(new Message(new Header(2, 0, Message.Type.USER, 1), Unpooled.buffer().writeShort(1).writeInt(1)));
    }

    @Test
    public void testStreamContextTouch() {
        StreamContext context = new StreamContext("test", new DefaultEventExecutor());
        Assert.assertSame(context, context.touch());
    }
}