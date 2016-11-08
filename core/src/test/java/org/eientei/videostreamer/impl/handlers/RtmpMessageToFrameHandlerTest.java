package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutor;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.tracks.TrackVideoH264Test;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.eientei.videostreamer.impl.tracks.TrackAudioAacTest.makeAudio;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
@RunWith(MockitoJUnitRunner.class)
public class RtmpMessageToFrameHandlerTest {
    @Mock
    private EventExecutor executor;

    @Test
    public void testMessageToSampleMetadata() {
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageToFrameHandler(new DefaultChannelGroup(executor), 500));

        channel.writeOutbound(makeMeta());

        RtmpMessageToFrameHandler handler = channel.pipeline().get(RtmpMessageToFrameHandler.class);

        Assert.assertEquals(30, handler.getFps());
        Assert.assertEquals(640, handler.getWidth());
        Assert.assertEquals(320, handler.getHeight());
        Assert.assertNull(channel.readOutbound());
        channel.close();
    }

    @Test
    public void testMessageToSampleUser() {
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageToFrameHandler(new DefaultChannelGroup(executor), 500));
        channel.writeOutbound(makeMeta());
        RtmpMessageToFrameHandler handler = channel.pipeline().get(RtmpMessageToFrameHandler.class);
        Assert.assertEquals(30, handler.getFps());
        Assert.assertEquals(640, handler.getWidth());
        Assert.assertEquals(320, handler.getHeight());

        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(1);
        buf.writeInt(1);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.USER, 0), buf));
        Assert.assertEquals(0, handler.getFps());
        Assert.assertEquals(0, handler.getWidth());
        Assert.assertEquals(0, handler.getHeight());
        Assert.assertNull(channel.readOutbound());
        channel.close();
    }
    @Test
    public void testMessageToSampleAV() {
        EmbeddedChannel channel = new EmbeddedChannel(new RtmpMessageToFrameHandler(new DefaultChannelGroup(executor), 500));
        channel.writeOutbound(makeMeta());
        Assert.assertNull(channel.readOutbound());
        channel.writeOutbound(TrackVideoH264Test.makeVideoInit(4, 0));
        Assert.assertNull(channel.readOutbound());
        channel.writeOutbound(makeAudio(0));
        Assert.assertNull(channel.readOutbound());
        channel.writeOutbound(TrackVideoH264Test.makeVideoSample(4, 1000));
        Assert.assertNull(channel.readOutbound());
        channel.writeOutbound(makeAudio(1000));
        Assert.assertNotNull(channel.readOutbound());

        channel.writeOutbound(makeAudio(1500));
        Assert.assertNull(channel.readOutbound());
        channel.writeOutbound(TrackVideoH264Test.makeVideoSample(4, 1500));
        Assert.assertNotNull(channel.readOutbound());

        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(1);
        buf.writeInt(1);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.USER, 0), buf));

        channel.close();
    }

    private Message makeMeta() {
        ByteBuf buf = Unpooled.buffer();
        Amf.serialize(buf,
                "onMetaData",
                Amf.makeObject(
                        "fps", 30.0,
                        "width", 640.0,
                        "height", 320.0
                )
        );
        Message message = new Message(new Header(3, 0, Message.Type.AMF0_META, 1), buf);
        buf.release();
        return message;
    }
}