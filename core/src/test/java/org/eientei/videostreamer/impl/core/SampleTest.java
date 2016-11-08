package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleTest {
    @Mock
    private Track track;

    @Test
    public void testSample() {
        ByteBuf data = Unpooled.buffer();
        data.writeByte(1 << 4);
        Message message = new Message(new Header(3, 0, Message.Type.VIDEO, 1), data);

        Sample sample = new Sample(message, 0, 1);

        Assert.assertSame(message, sample.getMessage());
        Assert.assertEquals(0, sample.getBegin());
        Assert.assertEquals(1, sample.getLength());
        Assert.assertEquals(0, sample.getBasetime());
        Assert.assertEquals(true, sample.isKey());

        message.release();

        Assert.assertEquals(1, message.refCnt());
        sample.release();
        Assert.assertEquals(0, message.refCnt());

        Assert.assertSame(sample, sample.touch());
    }
}
