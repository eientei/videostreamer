package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.amf.AmfList;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class MessageTest {
    @Test
    public void testMessage() {
        Header header = new Header(2, 0, Message.Type.ACK, 1);
        ByteBuf buffer = Unpooled.buffer();
        Message message = new Message(header, buffer);
        buffer.release();
        Assert.assertEquals(1, buffer.refCnt());

        Assert.assertEquals(header, message.getHeader());
        Assert.assertEquals(buffer, message.getData());
        message.release();
        Assert.assertEquals(0, buffer.refCnt());
        Assert.assertEquals(message, message.touch());
    }

    @Test
    public void testUnknown() {
        Assert.assertEquals(null, Amf.Type.dispatch(99));
    }

    @Test
    public void testMessageType() {
        Assert.assertEquals(1, Message.Type.SET_CHUNK_SIZE.getValue());
        Assert.assertEquals(3, Message.Type.ACK.getValue());
        Assert.assertEquals(4, Message.Type.USER.getValue());
        Assert.assertEquals(5, Message.Type.WINACK.getValue());
        Assert.assertEquals(6, Message.Type.SET_PEER_BAND.getValue());
        Assert.assertEquals(8, Message.Type.AUDIO.getValue());
        Assert.assertEquals(9, Message.Type.VIDEO.getValue());
        Assert.assertEquals(11, Message.Type.AMF3_CMD_ALT.getValue());
        Assert.assertEquals(15, Message.Type.AMF3_META.getValue());
        Assert.assertEquals(17, Message.Type.AMF3_CMD.getValue());
        Assert.assertEquals(18, Message.Type.AMF0_META.getValue());
        Assert.assertEquals(20, Message.Type.AMF0_CMD.getValue());
    }

    @Test
    public void testAsAmf() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0);
        Amf.serialize(buffer, 1.0, "abc", true);
        Message message = new Message(new Header(2, 0, Message.Type.AMF3_META, 1), buffer);
        buffer.release();
        Assert.assertEquals(1, buffer.refCnt());


        AmfList list1 = message.asAmf();
        AmfList list2 = message.asAmf();
        Assert.assertSame(list1, list2);
        Assert.assertEquals(1.0, list1.get(0));
        Assert.assertEquals("abc", list1.get(1));
        Assert.assertEquals(true, list1.get(2));

        message.release();
        Assert.assertEquals(0, buffer.refCnt());
    }

    @Test
    public void testRewind() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(0);
        Message message = new Message(new Header(2, 0, Message.Type.AMF0_META, 1), buffer);
        buffer.release();
        Assert.assertEquals(1, buffer.refCnt());

        Assert.assertEquals(4, message.getData().readableBytes());
        message.getData().readInt();
        Assert.assertEquals(4, message.getData().readableBytes());

        ByteBuf data = message.getData();
        Assert.assertEquals(4, data.readableBytes());
        data.readInt();
        Assert.assertEquals(0, data.readableBytes());

        message.release();
        Assert.assertEquals(0, buffer.refCnt());
    }
}
