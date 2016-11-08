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
public class FrameTest {
    @Mock
    private Track track1;

    @Mock
    private Track track2;

    @Test
    public void testFrameFirst() {
        Mockito.when(track1.getFrametick()).thenReturn(10);
        Mockito.when(track2.getFrametick()).thenReturn(20);

        Frame first = new Frame(makeList(track1), makeList(track2));

        Assert.assertEquals(0, first.getAudioSequence());
        Assert.assertEquals(0, first.getVideoSequence());
        Assert.assertEquals(2, first.getAudioList().getSamples().size());
        Assert.assertEquals(2, first.getVideoList().getSamples().size());

        Frame second = new Frame(first, makeList(track1), makeList(track2));

        Assert.assertEquals(2, second.getAudioList().getSamples().size());
        Assert.assertEquals(2, second.getVideoList().getSamples().size());

        Assert.assertEquals(2, second.getAudioSequence());
        Assert.assertEquals(2, second.getVideoSequence());

        second.release();

        Assert.assertSame(first, first.touch());
        Assert.assertSame(second, second.touch());
    }

    public SampleList makeList(Track track) {
        List<Sample> samples = new ArrayList<>();

        ByteBuf data1 = Unpooled.buffer();
        data1.writeByte(1<<4);
        Message message1 = new Message(new Header(3, 0, Message.Type.VIDEO, 1), data1);
        data1.release();

        ByteBuf data2 = Unpooled.buffer();
        data2.writeByte(1<<4);
        Message message2 = new Message(new Header(3, 0, Message.Type.VIDEO, 1), data2);
        data2.release();


        Sample sample1 = new Sample(message1, 0, 0);
        Sample sample2 = new Sample(message2, 0, 0);
        samples.add(sample1);
        samples.add(sample2);
        message1.release();
        message2.release();

        return new SampleList(track.getFrametick(), samples);
    }
}