package org.eientei.videostreamer.impl.tracks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.Sample;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class TrackAudioAacTest {

    @Test
    public void testConstruct() {
        ByteBuf adata = Unpooled.buffer();
        adata.writeShort(0);
        Message audio = new Message(new Header(3, 0, Message.Type.AUDIO, 1), adata);
        adata.release();
        TrackAudioAac aac = new TrackAudioAac(audio, 44100, 1024);
        audio.release();

        Assert.assertEquals(2, aac.getChannels());

        Assert.assertEquals(1, aac.getInit().refCnt());
        aac.release();
    }

    public static Message makeAudio(int time) {
        ByteBuf adata = Unpooled.buffer();
        adata.writeByte(10 << 4);
        adata.writeByte(0);
        adata.writeShort(0);
        Message audio = new Message(new Header(3, time, Message.Type.AUDIO, 1), adata);
        adata.release();
        return audio;
    }

    @Test
    public void testSample() throws Exception {
        Message audio = makeAudio(0);
        TrackAudioAac aac = new TrackAudioAac(audio, 44100, 1024);
        audio.release();

        ByteBuf sampledata = Unpooled.buffer();
        sampledata.writeShort(0);
        sampledata.writeInt(1);
        Message samplemsg = new Message(new Header(3, 0, Message.Type.AUDIO, 1), sampledata);

        Sample sample = aac.makeSample(samplemsg);

        Assert.assertEquals(1, sample.getData().readInt());
        aac.release();
    }
}
