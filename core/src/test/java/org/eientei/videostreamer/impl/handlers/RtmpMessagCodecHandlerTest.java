package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class RtmpMessagCodecHandlerTest {
    @Test
    public void testRtmpMessageEncoder() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());

        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();
        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);
        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderCompressNone() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());

        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();
        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        res = channel.readOutbound();
        channel.writeInbound(res);
        decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderCompressSmall() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();
        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        message = new Message(new Header(3, 10, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        res = channel.readOutbound();

        channel.writeInbound(res);
        decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderCompressMedium() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        ByteBuf res= channel.readOutbound();
        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        message = new Message(new Header(3, 10, Message.Type.VIDEO, 1), Unpooled.wrappedBuffer(new byte[] {  0x00 }));
        channel.writeOutbound(message);
        res = channel.readOutbound();

        channel.writeInbound(res);
        decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderCompressFull() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        channel.readOutbound();

        message = new Message(new Header(3, 0xFFFFFF, Message.Type.VIDEO, 1), Unpooled.wrappedBuffer(new byte[] {  0x00 }));
        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();

        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderChunked() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        ByteBuf buffer = Unpooled.buffer(513);
        buffer.writerIndex(513);
        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), buffer);

        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();

        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderChunkSize() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(65, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();

        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }


    @Test
    public void testRtmpMessageEncoderChunkSizeMore() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        Message message = new Message(new Header(390, 0, Message.Type.VIDEO, 1), Unpooled.buffer());
        channel.writeOutbound(message);
        ByteBuf res = channel.readOutbound();

        channel.writeInbound(res);
        Message decoded = channel.readInbound();
        Assert.assertEquals(message, decoded);

        Assert.assertEquals(0, res.refCnt());
        channel.close().awaitUninterruptibly();
    }

    @Test
    public void testRtmpMessageEncoderChunkSizeChange() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler());
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(2048);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.SET_CHUNK_SIZE, 0), buffer));
    }

    @Test
    public void testRtmpMessageDecoderChunkSizeChange() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(2048);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.SET_CHUNK_SIZE, 0), buffer));
        ByteBuf buf = channel.readOutbound();
        channel.writeInbound(buf);
    }

    @Test
    public void testRtmpMessageDecoderAckwindow() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new RtmpMessageEncoderHandler(), new RtmpMessageDecoderHandler());

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(5);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.WINACK, 0), buffer));
        channel.writeInbound(channel.readOutbound());
        channel.readInbound();

        ByteBuf data = Unpooled.buffer();
        data.writeByte(0);
        channel.writeOutbound(new Message(new Header(2, 0, Message.Type.VIDEO, 0), data));
        channel.writeInbound(channel.readOutbound());
        channel.readInbound();

        channel.writeInbound(channel.readOutbound());
        Message ack = channel.readInbound();
        Assert.assertEquals(5, ack.getData().getInt(0));
    }
}