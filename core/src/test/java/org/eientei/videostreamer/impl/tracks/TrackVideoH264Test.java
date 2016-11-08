package org.eientei.videostreamer.impl.tracks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.Sample;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class TrackVideoH264Test {

    @Test
    public void testConstruct() throws IOException {
        TrackVideoH264 h264 = makeh264(4);

        Assert.assertEquals(4, h264.getNalLenlen());
        Assert.assertEquals(4, h264.getFrameNumbits());

        Assert.assertEquals(1, h264.getInit().refCnt());
        h264.release();
        Assert.assertEquals(0, h264.getInit().refCnt());

        Assert.assertEquals(0, h264.getInit().refCnt());
    }

    @Test
    public void testMakeSampleSei() throws Exception {
        TrackVideoH264 h264 = makeh264(4);

        ByteBuf sampledata = Unpooled.buffer();
        sampledata.writeInt(0);
        sampledata.writeByte(0);

        sampledata.writeInt(1);
        sampledata.writeByte(1);
        sampledata.writeInt(1);
        sampledata.writeByte(6);
        sampledata.writeInt(1);
        sampledata.writeByte(1);
        Message samplemsg = new Message(new Header(3, 0, Message.Type.VIDEO, 1), sampledata);
        sampledata.release();

        Sample sample = h264.makeSample(samplemsg);
        ByteBuf sdat = sample.getData();
        Assert.assertEquals(1, sdat.readInt());
        Assert.assertEquals(1, sdat.readByte());
        Assert.assertFalse(sdat.isReadable());
    }

    @Test
    public void testMakeSampleSlice() throws Exception {
        TrackVideoH264 h264 = makeh264(4);

        ByteBuf sampledata = Unpooled.buffer();
        sampledata.writeInt(0);
        sampledata.writeByte(0);

        sampledata.writeInt(1);
        sampledata.writeByte(1);
        sampledata.writeInt(1);
        sampledata.writeByte(12);
        sampledata.writeInt(1);
        sampledata.writeByte(1);
        Message samplemsg = new Message(new Header(3, 0, Message.Type.VIDEO, 1), sampledata);
        sampledata.release();

        Sample sample = h264.makeSample(samplemsg);
        ByteBuf sdat = sample.getData();
        Assert.assertEquals(1, sdat.readInt());
        Assert.assertEquals(1, sdat.readByte());
        Assert.assertFalse(sdat.isReadable());
    }


    @Test
    public void testMakeSample3() throws Exception {
        testSample(3);
    }

    @Test
    public void testMakeSample2() throws Exception {
        testSample(2);
    }

    @Test
    public void testMakeSample1() throws Exception {
        testSample(1);
    }

    private void testSample(int len) throws Exception {
        TrackVideoH264 h264 = makeh264(len);
        Message vs = makeVideoSample(len, 0);
        Sample sample = h264.makeSample(vs);
        vs.release();
        ByteBuf sdat = sample.getData();
        int ret = 0;
        switch (len) {
            case 1:
                ret = sdat.readByte();
                break;
            case 2:
                ret = sdat.readShort();
                break;
            case 3:
                ret = sdat.readMedium();
                break;
            case 4:
                ret = sdat.readInt();
                break;
        }
        Assert.assertEquals(1, ret);
        Assert.assertEquals(1, sdat.readByte());
        Assert.assertFalse(sdat.isReadable());
    }

    public static Message makeVideoSample(int len, int time) {
        ByteBuf sampledata = Unpooled.buffer();
        sampledata.writeInt(0);
        sampledata.writeByte(0);

        switch (len) {
            case 1:
                sampledata.writeByte(1);
                break;
            case 2:
                sampledata.writeShort(1);
                break;
            case 3:
                sampledata.writeMedium(1);
                break;
            case 4:
                sampledata.writeInt(1);
                break;
        }
        sampledata.writeByte(1);
        Message samplemsg = new Message(new Header(3, time, Message.Type.VIDEO, 1), sampledata);
        sampledata.release();
        return samplemsg;
    }

    public static Message makeVideoInit(int len, int time) {
        ByteBuf vdata = Unpooled.buffer();
        vdata.writeByte(7);
        vdata.writeInt(0);

        vdata.writeInt(0);
        vdata.writeByte((len & 0x03) - 1);
        vdata.writeInt(0);
        vdata.writeShort(0);
        vdata.writeByte(1<<6 | 1<<4);
        Message video = new Message(new Header(3, time, Message.Type.VIDEO, 1), vdata);
        vdata.release();
        return video;
    }

    private TrackVideoH264 makeh264(int len) throws IOException {
        Message vi = makeVideoInit(len, 0);
        TrackVideoH264 h264 = new TrackVideoH264(vi, 30, 640, 320);
        vi.release();
        Assert.assertEquals(640, h264.getWidth());
        Assert.assertEquals(320, h264.getHeight());
        return h264;
    }
}