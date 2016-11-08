package org.eientei.videostreamer.impl.core;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.exceptions.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class GlobalContextTest {
    private static final String STREAM_NAME = "streamname";

    private Channel channel = new EmbeddedChannel();
    private Channel anotherChannel = new EmbeddedChannel();

    @Test
    public void testPublishOk() {
        GlobalContext context = new GlobalContext();
        try {
            context.publish(STREAM_NAME, channel);
        } catch (StreamAlreadyPublishingException e) {
            Assert.fail();
        }

        Assert.assertEquals(channel, context.stream(STREAM_NAME).getPublisher());
        Assert.assertEquals(1, context.allStreams().size());
        Assert.assertTrue(context.allStreams().contains(context.stream(STREAM_NAME)));
    }

    @Test
    public void testAlreadyPublishingFail() {
        GlobalContext context = new GlobalContext();
        try {
            context.publish(STREAM_NAME, channel);
        } catch (StreamAlreadyPublishingException e) {
            Assert.fail();
        }

        try {
            context.publish(STREAM_NAME, anotherChannel);
            Assert.fail();
        } catch (StreamAlreadyPublishingException ignore) {
        }
    }

    @Test
    public void testUnpublishOk() {
        GlobalContext context = new GlobalContext();
        EmbeddedChannel channel = new EmbeddedChannel();

        try {
            context.publish(STREAM_NAME, channel);
        } catch (StreamAlreadyPublishingException e) {
            Assert.fail();
        }

        Assert.assertEquals(1, context.allStreams().size());
        Assert.assertTrue(context.allStreams().contains(context.stream(STREAM_NAME)));

        channel.close();

        Assert.assertEquals(null, context.stream(STREAM_NAME));
        Assert.assertEquals(0, context.allStreams().size());
    }

    @Test
    public void testSubscribeOk() {
        GlobalContext context = new GlobalContext();

        try {
            context.subscribe(STREAM_NAME, channel);
        } catch (StreamAlreadySubscribedException e) {
            Assert.fail();
        }

        Assert.assertTrue(context.stream(STREAM_NAME).containsRtmpSubscriber(channel));
        Assert.assertEquals(1, context.allStreams().size());
        Assert.assertTrue(context.stream(STREAM_NAME).containsRtmpSubscriber(channel));
    }

    @Test
    public void testSubscribeFail() {
        GlobalContext context = new GlobalContext();

        try {
            context.subscribe(STREAM_NAME, channel);
        } catch (StreamAlreadySubscribedException e) {
            Assert.fail();
        }

        try {
            context.subscribe(STREAM_NAME, channel);
            Assert.fail();
        } catch (StreamAlreadySubscribedException ignore) {
        }
    }

    @Test
    public void testUnsubscribeOk() {
        GlobalContext context = new GlobalContext();
        EmbeddedChannel channel = new EmbeddedChannel();

        try {
            context.subscribe(STREAM_NAME, channel);
        } catch (StreamAlreadySubscribedException e) {
            Assert.fail();
        }

        Assert.assertEquals(1, context.allStreams().size());
        Assert.assertTrue(context.stream(STREAM_NAME).containsRtmpSubscriber(channel));
        Assert.assertEquals(null, context.stream(STREAM_NAME).getPublisher());

        channel.close();

        Assert.assertEquals(null, context.stream(STREAM_NAME));
    }

    @Test
    public void testUnsubscribeNotSubscribedFail() {
        GlobalContext context = new GlobalContext();
        EmbeddedChannel anotherChannel = new EmbeddedChannel();

        try {
            context.subscribe(STREAM_NAME, channel);
        } catch (StreamAlreadySubscribedException e) {
            Assert.fail();
        }

        anotherChannel.close();
    }

    @Test
    public void testChannelElimination() {

        GlobalContext context = new GlobalContext();
        EmbeddedChannel channel = new EmbeddedChannel();

        EmbeddedChannel anotherChannel = new EmbeddedChannel();

        Assert.assertEquals(0, context.allStreams().size());

        try {
            context.publish(STREAM_NAME, channel);
        } catch (StreamAlreadyPublishingException e) {
            Assert.fail();
        }

        Assert.assertEquals(1, context.allStreams().size());

        try {
            context.subscribe(STREAM_NAME, anotherChannel);
        } catch (StreamAlreadySubscribedException e) {
            Assert.fail();
        }

        Assert.assertEquals(1, context.allStreams().size());

        anotherChannel.close();

        Assert.assertEquals(1, context.allStreams().size());

        channel.close();

        Assert.assertEquals(0, context.allStreams().size());
    }
}