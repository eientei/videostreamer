package org.eientei.videostreamer.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.message.*;
import org.eientei.videostreamer.ws.CommType;

import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class StreamContext {
    private final Mp4RemuxerHandler muxer;

    private class Broadcaster extends SimpleChannelInboundHandler<RtmpMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RtmpMessage msg) throws Exception {
            switch (msg.getType()) {
                case AUDIO:
                    process((RtmpAudioMessage)msg);
                    break;
                case VIDEO:
                    process((RtmpVideoMessage)msg);
                    break;
                case AMF3_META:
                case AMF0_META:
                    process((RtmpMetaMessage)msg);
                    break;
            }
        }
    };

    private final ChannelGroup rtmpGroup;
    private final ChannelGroup remuxGroup;
    private Channel publisher;
    private ByteBuf metaData;
    private ByteBuf videoData;
    private ByteBuf audioData;
    private boolean initialized;

    public StreamContext(ServerContext serverContext) {
        this.rtmpGroup = new DefaultChannelGroup(serverContext.getRtmpExecutor());
        this.remuxGroup = new DefaultChannelGroup(serverContext.getRemuxExecutor());
        muxer = new Mp4RemuxerHandler(remuxGroup);
        final EmbeddedChannel remuxChannel = new EmbeddedChannel(
                DefaultChannelId.newInstance(),
                muxer
        );
        rtmpGroup.add(remuxChannel);
    }

    public void checkBoot() {
        if (initialized) {
            return;
        }
        if (metaData.isReadable() && videoData.isReadable() && audioData.isReadable()) {
            initialized = true;
            for (Channel ch : rtmpGroup) {
                bootstrap(ch);
            }
        }
    }

    public boolean isPublishing() {
        return publisher != null;
    }

    public boolean publishRtmp(final Channel channel) {
        if (isPublishing()) {
            return false;
        }

        channel.pipeline().addLast(new Broadcaster());
        channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                unpublishRtmp(channel);
            }
        });
        publisher = channel;

        for (Channel ch : rtmpGroup) {
            begin(ch);
        }

        metaData = channel.alloc().buffer();
        videoData = channel.alloc().buffer();
        audioData = channel.alloc().buffer();

        return true;
    }

    public boolean unpublishRtmp(Channel channel) {
        if (channel != publisher) {
            return false;
        }

        channel.pipeline().remove(Broadcaster.class);

        initialized = false;
        publisher = null;
        metaData.release();
        videoData.release();
        audioData.release();

        for (Channel ch : rtmpGroup) {
            end(ch);
        }

        rtmpGroup.remove(channel);
        return true;
    }

    public boolean subscrieRtmp(Channel channel) {
        rtmpGroup.add(channel);
        begin(channel);
        bootstrap(channel);
        return true;
    }

    public boolean unsubscrieRtmp(Channel channel) {
        rtmpGroup.remove(channel);
        end(channel);
        return true;
    }

    private void begin(Channel channel) {
        channel.writeAndFlush(new RtmpUserMessage(2, 0, 0, channel.alloc().buffer(),
                RtmpUserMessage.Event.STREAM_BEGIN,
                1,
                0
        ));
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, channel.alloc().buffer(),
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.Start",
                        "description", "Start live."
                )
        ));
        channel.writeAndFlush(new RtmpMetaMessage(5, 1, 0, channel.alloc().buffer(),
                "|RtmpSampleAccess",
                true,
                true
        ));
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, channel.alloc().buffer(),
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.PublishNotify",
                        "description", "Start publishing."
                )
        ));
    }

    private void end(Channel channel) {
        channel.writeAndFlush(new RtmpUserMessage(2, 0, 0, channel.alloc().buffer(), RtmpUserMessage.Event.STREAM_EOF, 1, 0));
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, channel.alloc().buffer(),
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.Stop",
                        "description", "Stop live."
                )
        ));
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, channel.alloc().buffer(),
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.UnpublishNotify",
                        "description", "Stop publishing."
                )
        ));
    }

    private void bootstrap(Channel channel) {
        if (metaData.isReadable() && videoData.isReadable() && audioData.isReadable()) {
            channel.writeAndFlush(new RtmpMetaMessage(5, 1, 0, metaData.retain()));
            channel.writeAndFlush(new RtmpVideoMessage(6, 1, 0, videoData.retain()));
            channel.writeAndFlush(new RtmpAudioMessage(4, 1, 0, audioData.retain()));
        }
    }

    public void process(RtmpMetaMessage message) {
        AmfListWrapper amf = Amf.deserializeAll(message.getData());
        Map<String, Object> data = amf.get(2);

        if (!metaData.isReadable() ) {
            // sideeffect
            new RtmpMetaMessage(5, 1, 0, metaData, "onMetaData", Amf.makeObject(
                    "videocodecid", 0.0,
                    "audiocodecid", 0.0,
                    "videodatarate", data.get("videodatarate"),
                    "audiodatarate", data.get("audiodatarate"),
                    "duration", 0.0,
                    "framerate", data.get("framerate"),
                    "fps", data.get("framerate"),
                    "width", data.get("width"),
                    "height", data.get("height"),
                    "displaywidth", data.get("width"),
                    "height", data.get("height"))).getData();
        } else {
            rtmpGroup.writeAndFlush(message.retain());
        }
    }

    public void process(RtmpVideoMessage message) {
        if (!videoData.isReadable() && message.getData().getByte(1) == 0) {
            videoData.ensureWritable(message.getData().readableBytes());
            videoData.writeBytes(message.getData());
            checkBoot();
        } else {
            rtmpGroup.writeAndFlush(message.retain());
        }
    }

    public void process(RtmpAudioMessage message) {
        if (!audioData.isReadable() && message.getData().getByte(1) == 0) {
            audioData.ensureWritable(message.getData().readableBytes());
            audioData.writeBytes(message.getData());
            checkBoot();
        } else {
            rtmpGroup.writeAndFlush(message.retain());
        }
    }

    public boolean subscribeMuxer(Channel channel) {
        remuxGroup.add(channel);
        notifySubscribers(channel, remuxGroup);
        muxer.subscribed(channel);
        return true;
    }

    public boolean unsubscribeMuxer(Channel channel) {
        remuxGroup.remove(channel);
        notifySubscribers(channel, remuxGroup);
        return true;
    }

    private void notifySubscribers(Channel channel, ChannelGroup remuxGroup) {
        ByteBuf subbuf = channel.alloc().heapBuffer();
        subbuf.writeInt(CommType.STREAM_SUBSCRIBERS.getNum());
        subbuf.writeInt(remuxGroup.size());
        remuxGroup.writeAndFlush(subbuf);
    }
}
