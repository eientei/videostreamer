package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.h264.PpsNalUnit;
import org.eientei.videostreamer.h264.SpsNalUnit;
import org.eientei.videostreamer.mp4.boxes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4VideoTrack extends Mp4Track {
    public ByteBuf avc;
    public short confver;
    public short profile;
    public short profileCompat;
    public short avcLevel;
    public int nalBytes;
    public SpsNalUnit sps;
    public PpsNalUnit pps;
    public List<Mp4Sample> samples = new ArrayList<>();
    public boolean prestine = true;
    private long preticks;
    private int lastFrameNum = -1;
    private Queue<Mp4TrackFrame> frames = new LinkedBlockingQueue<>();


    public Mp4VideoTrack(Mp4Context context, ByteBuf data) {
        super(context, "vide", "VideoHandler", new VmhdBox(context));

        avc = data.copy(data.readerIndex(), data.readableBytes());
        confver = data.readUnsignedByte();
        profile = data.readUnsignedByte();
        profileCompat = data.readUnsignedByte();
        avcLevel = data.readUnsignedByte();
        nalBytes = (data.readUnsignedByte() & 0x03) + 1;
        int spscount = data.readUnsignedByte() & 0x1f; // equal to 1
        int spslength = data.readUnsignedShort();
        sps = new SpsNalUnit(data, data.readableBytes());
        data.skipBytes(spslength);
        int ppscount = data.readUnsignedByte(); // equal to 1
        int ppslength = data.readUnsignedShort();
        pps = new PpsNalUnit(data, ppslength);

        data.resetReaderIndex();
        init = new Avc1Box(context, this);
        //volume = 0;
        //width = sps.vui.sarWidth;
        //height = sps.vui.sarHeight;
        //timescale = sps.vui.timeScale >> 1;
    }

    private Mp4TrackFrame createFrame() {
        Mp4TrackFrame mp4 = new Mp4TrackFrame(this, samples);
        samples.clear();
        ticks = preticks;
        return mp4;
    }

    @Override
    public Box getInitBox() {
        return null;
    }

    public void addSample(Mp4VideoSample sample) {
        if (sample.getSlice().nalUnitType == 6) {
            return;
        }

        if (lastFrameNum != -1 && lastFrameNum != sample.getSlice().frameNum) {
            //if (samples.size() * frametick >= timescale) {
                Mp4TrackFrame frame = createFrame();
                frames.add(frame);
            //}
            preticks += frametick;
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(sample.getData().readableBytes());
            buf.writeBytes(sample.getData());
            samples.add(new Mp4VideoSample(sample.getSlice(), buf));
        } else if (!samples.isEmpty()) {
            ByteBuf naldata = samples.get(samples.size()-1).getData();
            naldata.writeInt(sample.getData().readableBytes());
            naldata.writeBytes(sample.getData());
        } else {
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(sample.getData().readableBytes());
            buf.writeBytes(sample.getData());
            samples.add(new Mp4VideoSample(sample.getSlice(), buf));
        }
        lastFrameNum = sample.getSlice().frameNum;

    }

    @Override
    public boolean isCompleteFrame() {
        return !frames.isEmpty();
    }

    @Override
    public Mp4TrackFrame getFrame() {
        return frames.poll();
    }
}
