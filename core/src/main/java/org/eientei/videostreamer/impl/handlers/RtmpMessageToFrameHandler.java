package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.group.ChannelGroup;
import org.eientei.videostreamer.impl.amf.AmfList;
import org.eientei.videostreamer.impl.core.*;
import org.eientei.videostreamer.impl.mp4.Box;
import org.eientei.videostreamer.impl.tracks.TrackAudioAac;
import org.eientei.videostreamer.impl.tracks.TrackVideoH264;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-11-05
 */
public class RtmpMessageToFrameHandler extends ChannelOutboundHandlerAdapter {
    private final static int VIDEO_FORMAT_H264 = 7;
    private final static int AUDIO_FORMAT_MP3 = 2;
    private final static int AUDIO_FORMAT_AAC = 10;
    private final static int[] aacRates = new int[] {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000};
    private final ChannelGroup group;
    private final int threshold;

    private int fps;
    private int width;
    private int height;
    private Track video;
    private Track audio;
    private List<Sample> alist = new ArrayList<>();
    private List<Sample> vlist = new ArrayList<>();
    private int time = -1;
    private Frame frame;
    private ByteBuf init;

    public RtmpMessageToFrameHandler(ChannelGroup group, int threshold) {
        this.group = group;
        this.threshold = threshold;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object rawmsg, ChannelPromise promise) throws Exception {
        Message msg = (Message) rawmsg;
        try {
            switch (msg.getHeader().getType()) {
                case AMF0_META:
                case AMF3_META:
                    parseMeta(msg);
                    break;
                case AUDIO:
                    parseAudio(ctx, msg);
                    break;
                case VIDEO:
                    parseVideo(ctx, msg);
                    break;
                case USER:
                    parseUser(msg);
                    break;
            }
            if (time == -1) {
                time = msg.getHeader().getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        msg.release();
    }

    private void parseUser(Message msg) {
        if (msg.getData().getShort(0) == 1) {
            cleanup();
        }
    }

    public void cleanup() {
        if (video != null) {
            video.release();
        }
        if (audio != null) {
            audio.release();
        }
        if (frame != null) {
            frame.release();
        }
        if (init != null) {
            init.release();
            init = null;
        }
        fps = 0;
        width = 0;
        height = 0;
        for (Sample sample : alist) {
            sample.release();
        }
        alist.clear();
        for (Sample sample : vlist) {
            sample.release();
        }
        vlist.clear();
    }

    private void parseVideo(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (video == null) {
            ByteBuf data = msg.getData();
            int fstvid = data.readByte();
            int videocodec = fstvid & 0x0f;
            switch (videocodec) {
                case VIDEO_FORMAT_H264:
                    video = new TrackVideoH264(msg, fps, width, height);
                    break;
            }
            tryInit();
        } else {
            Sample sample = video.makeSample(msg);
            if (sample != null) {
                vlist.add(sample);
            }
        }
        if (msg.getHeader().getTime() - time >= threshold) {
            if (tryFrame(ctx)) {
                time = msg.getHeader().getTime();
            }
        }
    }

    private void parseAudio(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (audio == null) {
            ByteBuf data = msg.getData();
            int fstsnd = data.readUnsignedByte();
            int audiocodec = (fstsnd & 0xf0) >> 4;
            data.skipBytes(1);
            switch (audiocodec) {
                case AUDIO_FORMAT_AAC:
                    ByteBuf dt = data.slice();
                    int a = dt.readUnsignedByte();
                    int b = dt.readUnsignedByte();
                    int idx = ((a & 0b00000111) << 1) | ((b & 0b10000000) >> 7);
                    int sampleRate = aacRates[idx];
                    int frameLenFlag = (b & 0b00000100) >> 2;
                    int sampleCount = (frameLenFlag == 1 ? 960 : 1024);
                    audio = new TrackAudioAac(msg, sampleRate, sampleCount);
                    break;
            }
            tryInit();
        } else {
            Sample sample = audio.makeSample(msg);
            if (sample != null) {
                alist.add(sample);
            }
        }
        if (msg.getHeader().getTime() - time >= threshold) {
            if (tryFrame(ctx)) {
                time = msg.getHeader().getTime();
            }
        }
    }

    private void tryInit() {
        if (audio != null && video != null && init == null) {
            init = Unpooled.buffer();
            Box.ftyp(init);
            Box.moov(init, audio, video);
            init.retain();
            group.writeAndFlush(init);
        }
    }

    private boolean tryFrame(ChannelHandlerContext ctx) {
        if (alist.isEmpty() || vlist.isEmpty()) {
            return false;
        }
        if (group.isEmpty()) {
            for (Sample sample : alist) {
                sample.release();
            }
            alist.clear();
            for (Sample sample : vlist) {
                sample.release();
            }
            vlist.clear();
            return true;
        }
        SampleList al = new SampleList(audio.getFrametick(), alist);
        SampleList vl = new SampleList(video.getFrametick(), vlist);
        alist.clear();
        vlist.clear();
        if (frame == null) {
            frame = new Frame(al, vl);
        } else {
            frame = new Frame(frame, al, vl);
        }
        frame.retain();
        BinaryFrame binaryFrame = new BinaryFrame(ctx.alloc(), frame);
        group.writeAndFlush(binaryFrame);
        return true;
    }

    private void parseMeta(Message msg) {
        AmfList list = msg.asAmf();
        String meth = list.getAs(0);
        if (!meth.equalsIgnoreCase("@setDataFrame")) {
            return;
        }
        HashMap<String,Object> map = list.getAs(2);
        fps = ((Number)map.get("framerate")).intValue();
        width = ((Number)map.get("width")).intValue();
        height = ((Number)map.get("height")).intValue();
    }

    public int getFps() {
        return fps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuf getInit() {
        return init;
    }
}
