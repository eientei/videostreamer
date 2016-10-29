package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.eientei.videostreamer.aac.AacHeader;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.mp4.boxes.Mp4FtypBox;
import org.eientei.videostreamer.mp4.boxes.Mp4MoovBox;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.message.RtmpUserMessage;
import org.eientei.videostreamer.ws.CommType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-28
 */
public class Mp4RemuxerHandler extends MessageToByteEncoder<RtmpMessage> {
    private final static int VIDEO_FORMAT_H264 = 7;
    private final static int AUDIO_FORMAT_MP3 = 2;
    private final static int AUDIO_FORMAT_AAC = 10;

    private final static AttributeKey<Mp4SubscriberContext> CONTEXT = AttributeKey.newInstance("mp4.subscriber.context");

    private final ChannelGroup remuxedSubscribers;
    private final List<Mp4Track> tracks = new ArrayList<>();
    private int id = 1;
    private StringBuilder codecs;
    private AmfObjectWrapper metadata;
    private Mp4Track audio;
    private Mp4Track video;
    private long lastsent = System.currentTimeMillis();
    private boolean started = false;
    private FileOutputStream fos;

    public Mp4RemuxerHandler(ChannelGroup remuxedSubscribers) {
        this.remuxedSubscribers = remuxedSubscribers;
    }

    public Mp4SubscriberContext getSubscriberContext(final Channel channel) {
        if (channel.hasAttr(CONTEXT)) {
            return channel.attr(CONTEXT).get();
        }
        Mp4SubscriberContext context = new Mp4SubscriberContext();
        channel.attr(CONTEXT).set(context);
        return context;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void encode(ChannelHandlerContext ctx, RtmpMessage msg, ByteBuf out) throws Exception {
        switch (msg.getType()) {
            case USER:
                RtmpUserMessage user = (RtmpUserMessage) msg;
                switch (user.getEvent()) {
                    case STREAM_BEGIN:
                        codecs = new StringBuilder();
                        break;
                    case STREAM_EOF:
                        for (Mp4Track track : tracks) {
                            track.release();
                        }
                        tracks.clear();
                        started = false;
                        audio = null;
                        video = null;
                        metadata = null;

                        ByteBuf stopbuf = ctx.alloc().heapBuffer();
                        stopbuf.writeInt(CommType.STREAM_STOP.getNum());
                        remuxedSubscribers.writeAndFlush(stopbuf);
                        break;
                }
                break;
            case AUDIO:
                if (audio == null) {
                    tracks.add(audio = addAudioTrack(msg.getData()));
                    tryinit(ctx);
                } else if (audio != null) {
                    int fst = msg.getData().readUnsignedByte();
                    int audiocodec = (fst & 0xf0) >> 4;
                    int channels = (fst & 0x01) + 1;
                    int samplesiz = ((fst & 0x02) != 0) ? 2 : 1;
                    int samplerate = (fst & 0x0c) >> 2;
                    msg.getData().skipBytes(1);
                    audio.update(msg.getData(), msg.getTime(), true);
                    trysendFrame();
                }
                break;
            case VIDEO:
                if (video == null) {
                    tracks.add(video = addVideoTrack(msg.getData()));
                    tryinit(ctx);
                } else if (video != null) {
                    int fst = msg.getData().readByte();
                    int frametype = (fst & 0xf0) >> 4;
                    int videocodec = fst & 0x0f;
                    int avcpacktype = msg.getData().readByte();
                    int delay = msg.getData().readMedium();
                    video.update(msg.getData(), msg.getTime(), frametype == 1);
                    trysendFrame();
                }
                break;
            case AMF3_META:
            case AMF0_META:
                AmfListWrapper amf = Amf.deserializeAll(msg.getData());
                if (amf.get(0).equals("onMetaData")) {
                    metadata = new AmfObjectWrapper((Map<String, Object>) amf.get(1));
                    tryinit(ctx);
                }
                break;
        }
    }

    private void tryinit(ChannelHandlerContext ctx) {
        if (audio != null && video != null && !started && !remuxedSubscribers.isEmpty()) {
            try {
                fos = new FileOutputStream(new File("fos.mp4"));
            } catch (Exception ignore) {
            }
            started = true;
            ByteBuf playbuf = ctx.alloc().heapBuffer();
            playbuf.writeInt(CommType.STREAM_PLAY.getNum());
            playbuf.writeCharSequence(codecs, CharsetUtil.UTF_8);
            remuxedSubscribers.writeAndFlush(playbuf);
        }
    }

    private void trysendFrame() {
        if (remuxedSubscribers.isEmpty()) {
            return;
        }
        if ((audio != null && !audio.isSamplesReady()) || (video != null && !video.isSamplesReady())) {
            return;
        }
        if (System.currentTimeMillis() - lastsent < 500) {
            return;
        }

        lastsent = System.currentTimeMillis();
        Mp4Frame frame = new Mp4Frame();
        for (Mp4Track track : tracks) {
            frame.append(track);
        }

        for (Channel channel : remuxedSubscribers) {
            Mp4SubscriberContext context = getSubscriberContext(channel);
            if (context.getTracktimes().get(tracks.get(0)) == null) {
                for (Mp4Track track : tracks) {
                    context.getTracktimes().put(track, 0);
                    context.setBegin(frame.getMinTimestamp(tracks));
                }
                channel.pipeline().writeAndFlush(accept(channel, CommType.STREAM_UPDATE_AVK, 0, 0, makeHeader(tracks)));
            }

            CommType type = frame.isKeyframe(video) ? CommType.STREAM_UPDATE_AVK : CommType.STREAM_UPDATE_AV;
            int min = frame.getMinTimestamp(tracks) - context.getBegin();
            int max = frame.getMaxTimestamp(tracks) - context.getBegin();
            channel.pipeline().writeAndFlush(accept(channel, type, min, max, frame.getMoof(this, tracks, context), frame.getMdat(this, tracks)));
        }
        frame.release();
    }

    private ByteBuf accept(Channel channel, CommType type, int min, int max, Mp4Box... boxes) {
        ByteBuf buffer = channel.alloc().heapBuffer();
        buffer.writeInt(type.getNum());
        buffer.writeInt(min);
        buffer.writeInt(max);
        for (Mp4Box box : boxes) {
            box.write(buffer);
        }
        try {
            fos.write(buffer.array(), buffer.arrayOffset()+buffer.readerIndex()+12, buffer.readableBytes()-12);
        } catch (IOException ignore) {
        }
        return buffer;
    }

    private Mp4Box[] makeHeader(List<Mp4Track> tracks) {
        Mp4Box ftyp = new Mp4FtypBox(this, "mp42", 1, "mp42", "avc1", "iso5");
        Mp4Box moov = new Mp4MoovBox(this, tracks);
        return new Mp4Box[] { ftyp, moov };
    }

    private Mp4Track addVideoTrack(ByteBuf data) {
        int fst = data.readByte();
        int frametype = (fst & 0xf0) >> 4;
        int videocodec = fst & 0x0f;
        int avcpacktype = data.readByte();
        int delay = data.readMedium();
        switch (videocodec) {
            case VIDEO_FORMAT_H264:
                if (codecs.length() > 0) {
                    codecs.append(", ");
                }
                codecs.append("avc1.42C01F");
                return new Mp4VideoTrackH264(this, metadata, data.slice());
        }
        return null;
    }

    private Mp4Track addAudioTrack(ByteBuf data) {
        int fst = data.readUnsignedByte();
        int audiocodec = (fst & 0xf0) >> 4;
        int channels = (fst & 0x01) + 1;
        int samplesiz = ((fst & 0x02) != 0) ? 2 : 1;
        int samplerate = (fst & 0x0c) >> 2;
        data.skipBytes(1);
        switch (audiocodec) {
            case AUDIO_FORMAT_AAC:
                if (codecs.length() > 0) {
                    codecs.append(", ");
                }
                codecs.append("mp4a.40.2");
                AacHeader aacHeader = new AacHeader(data.slice());
                int sampleCount = (aacHeader.frameLenFlag == 1 ? 960 : 1024);
                return new Mp4AudioTrackAac(this, channels, samplesiz * 16, aacHeader.sampleRate, sampleCount, data.slice());
            case AUDIO_FORMAT_MP3:
                break;
        }
        return null;
    }

    public int getNextMoofId() {
        return id++;
    }

    public void subscribed(Channel channel) {
        if (started) {
            try {
                fos = new FileOutputStream(new File("fos.mp4"));
            } catch (Exception ignore) {
            }
            ByteBuf playbuf = channel.alloc().heapBuffer();
            playbuf.writeInt(CommType.STREAM_PLAY.getNum());
            playbuf.writeCharSequence(codecs, CharsetUtil.UTF_8);
            channel.writeAndFlush(playbuf);
        }
    }
}
