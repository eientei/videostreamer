package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.Base64Utils;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class RtmpHandshakeHandlerTest {
    private void testStage0(EmbeddedChannel channel, int code) {
        ByteBuf buffer = Unpooled.wrappedBuffer(new byte[] {(byte) code});
        channel.writeInbound(buffer);
        channel.readInbound();
        Assert.assertEquals(0, buffer.refCnt());
    }


    private void testStage1(EmbeddedChannel channel, String digest, int offset) {
        ByteBuf buffer = Unpooled.buffer(RtmpHandshakeHandler.HANDSHAKE_LENGTH);
        buffer.writerIndex(RtmpHandshakeHandler.HANDSHAKE_LENGTH);
        if (digest != null) {
            byte[] digestData = Base64Utils.decodeFromString(digest);
            buffer.setBytes(offset, digestData);
        }
        channel.writeInbound(buffer);
        channel.readInbound();
        ByteBuf buf = channel.readOutbound();
        buf.release();
        Assert.assertEquals(0, buffer.refCnt());
    }

    private void testStage2(EmbeddedChannel channel) {
        ByteBuf buffer = Unpooled.buffer(RtmpHandshakeHandler.HANDSHAKE_LENGTH);
        buffer.writerIndex(RtmpHandshakeHandler.HANDSHAKE_LENGTH);
        channel.writeInbound(buffer);
        channel.readInbound();
        ByteBuf buf = channel.readOutbound();
        buf.release();
        Assert.assertEquals(0, buffer.refCnt());
    }

    @Test
    public void testHandshakeStage0Fail() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler(null));
        testStage0(channel, 0x02);
        Assert.assertFalse(channel.isActive());
        Assert.assertFalse(channel.isOpen());
        channel.close().syncUninterruptibly();
    }

    @Test
    public void testHandshakeStage0Ok() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler(null));
        testStage0(channel, 0x03);
        Assert.assertTrue(channel.isActive());
        Assert.assertTrue(channel.isOpen());
        channel.close().syncUninterruptibly();
    }

    @Test
    public void testHandshakeStage1Old() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler(null));
        testStage0(channel, 0x03);
        testStage1(channel, null, 0);
        channel.close().syncUninterruptibly();
    }

    @Test
    public void testHandshakeStage1New() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler(null));
        testStage0(channel, 0x03);
        testStage1(channel, "SFaMo8lX0Z7dHRHVo9wiBfQy0jRko9y4SaIskYIqZdA=", 12);
        channel.close().syncUninterruptibly();
    }

    @Test
    public void testHandshakeStage2() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler(null));
        testStage0(channel, 0x03);
        testStage1(channel, null, 0);
        testStage2(channel);
        channel.close().syncUninterruptibly();
    }

    @Test
    public void testHandshakeStage2Init() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final boolean[] result = new boolean[] { false };
        channel.pipeline().addLast(new RtmpHandshakeHandler(new Runnable() {
            @Override
            public void run() {
                result[0] = true;
            }
        }));
        testStage0(channel, 0x03);
        testStage1(channel, null, 0);
        testStage2(channel);
        Assert.assertTrue(result[0]);
        channel.close().syncUninterruptibly();
    }

    /*
    @Test
    public void testHandshakeStage0Fail() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler());
        stage0(channel, 0x02);
        channel.finishAndReleaseAll();
    }

    @Test
    public void testHandshakeStage0Ok() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler());
        stage0(channel, 0x03);
        channel.finishAndReleaseAll();
    }

    @Test
    public void testHandshakeStage1Old() {
        EmbeddedChannel channel = new EmbeddedChannel();
        stage0(channel, 0x03);
        stage1(channel, null, 0);
        channel.finishAndReleaseAll();
    }

    @Test
    public void testHandhshakeStage1New() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpHandshakeHandler());
        stage0(channel, 0x03);
        byte[] digest = Base64Utils.decodeFromString("SFaMo8lX0Z7dHRHVo9wiBfQy0jRko9y4SaIskYIqZdA=");
        stage1(channel, digest, 12);
        channel.finishAndReleaseAll();
    }

    private void stage1(EmbeddedChannel channel, byte[] digest, int offset) {
        byte[] dataStage1 = new byte[RtmpHandshakeHandler.HANDSHAKE_LENGTH];
        ByteBuf buf = Unpooled.wrappedBuffer(dataStage1);
        if (digest != null) {
            buf.setBytes(offset, digest);
        }

        channel.writeInbound(buf);
        channel.flush();
    }

    private void stage0(EmbeddedChannel channel, int code) {
        ByteBuf data = Unpooled.buffer();
        data.writeByte(code);
        Assert.assertTrue(channel.isActive());
        channel.writeInbound(data);
        channel.flush();
        if (code == 0x03) {
            Assert.assertTrue(channel.isActive());
        } else {
            Assert.assertFalse(channel.isActive());
        }
    }

    @Test
    public void testHandshakeStage2() {
        EmbeddedChannel channel = new EmbeddedChannel();
        stage0(channel, 0x03);
        stage1(channel, null, 0);

        byte[] dataStage2 = new byte[RtmpHandshakeHandler.HANDSHAKE_LENGTH];
        ByteBuf buf = Unpooled.wrappedBuffer(dataStage2);
        channel.writeInbound(buf);
        channel.flush();

        channel.finishAndReleaseAll();
    }
    */
}