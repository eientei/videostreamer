package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleListTest {
    @Mock
    private Track track;

    @Test
    public void testConstruct() {
        Mockito.when(track.getFrametick()).thenReturn(10);
        ByteBuf data = Unpooled.buffer();
        data.writeInt(1<<4);
        Message message1 = new Message(new Header(3, 10, Message.Type.VIDEO, 1), data);
        Message message2 = new Message(new Header(3, 20, Message.Type.VIDEO, 1), data);
        data.release();

        List<Sample> samples = new ArrayList<>();
        Sample sample1 = new Sample(message1, 0, 1);
        Sample sample2 = new Sample(message2, 0, 1);
        message1.release();
        message2.release();
        samples.add(sample1);
        samples.add(sample2);
        SampleList sampleList = new SampleList(track.getFrametick(), samples);

        Assert.assertEquals(samples, sampleList.getSamples());
        Assert.assertEquals(10, sampleList.getEarliest());
        Assert.assertEquals(20, sampleList.getLattiest());
        Assert.assertSame(sampleList, sampleList.touch());
        Assert.assertEquals(track.getFrametick() * samples.size(), sampleList.getDuration());

        Assert.assertEquals(2, data.refCnt());
        sampleList.release();
        Assert.assertEquals(0, data.refCnt());
    }
}
