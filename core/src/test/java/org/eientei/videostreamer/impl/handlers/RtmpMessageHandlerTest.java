package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
@RunWith(MockitoJUnitRunner.class)
public class RtmpMessageHandlerTest {
    @Mock
    private GlobalContext globalContext;

    @Test
    public void testMessageHandlerConnect() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageHandler(globalContext));

        ByteBuf connect = Unpooled.buffer();
        Amf.serialize(connect,
                "connect",
                1.0
        );
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_CMD, 0), connect));
        Message winack = channel.readOutbound();
        Message peerband = channel.readOutbound();
        Message chunksiz = channel.readOutbound();
        Message echo = channel.readOutbound();

        Assert.assertEquals(Message.Type.WINACK, winack.getHeader().getType());
        Assert.assertEquals(Message.Type.SET_PEER_BAND, peerband.getHeader().getType());
        Assert.assertEquals(Message.Type.SET_CHUNK_SIZE, chunksiz.getHeader().getType());
        Assert.assertEquals(Message.Type.AMF0_CMD, echo.getHeader().getType());
    }

    @Test
    public void testMessageHandlerCreateStream() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageHandler(globalContext));

        ByteBuf createStream = Unpooled.buffer();
        Amf.serialize(createStream,
                "createStream",
                1.0
        );
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_CMD, 0), createStream));
        Message echo = channel.readOutbound();

        Assert.assertEquals(Message.Type.AMF0_CMD, echo.getHeader().getType());
    }

    @Test
    public void testMessageHandlerPublish() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageHandler(globalContext));

        ByteBuf publish = Unpooled.buffer();
        Amf.serialize(publish,
                "publish",
                1.0,
                null,
                "streamName"
        );
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_CMD, 0), publish));
        Message echo = channel.readOutbound();

        Assert.assertEquals(Message.Type.AMF0_CMD, echo.getHeader().getType());
    }

    @Test
    public void testMessageHandlerPlay() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageHandler(globalContext));

        ByteBuf play = Unpooled.buffer();
        Amf.serialize(play,
                "play",
                1.0,
                null,
                "streamName"
        );
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_CMD, 0), play));
    }

    @Test
    public void testMessageHandlerPass() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageHandler(globalContext));

        channel.writeInbound(new Message(new Header(2, 0, Message.Type.VIDEO, 1), Unpooled.buffer()));
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AUDIO, 1), Unpooled.buffer()));
        channel.writeInbound(new Message(new Header(2, 0, Message.Type.AMF0_META, 1), Unpooled.buffer()));

        Message video = channel.readInbound();
        Message audio = channel.readInbound();
        Message metad = channel.readInbound();

        Assert.assertEquals(Message.Type.VIDEO, video.getHeader().getType());
        Assert.assertEquals(Message.Type.AUDIO, audio.getHeader().getType());
        Assert.assertEquals(Message.Type.AMF0_META, metad.getHeader().getType());

        Assert.assertEquals(1, video.refCnt());
        Assert.assertEquals(1, audio.refCnt());
        Assert.assertEquals(1, metad.refCnt());

        video.release();
        audio.release();
        metad.release();
    }
}