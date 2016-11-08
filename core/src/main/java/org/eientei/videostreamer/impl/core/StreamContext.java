package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.handlers.RtmpMessageToFrameHandler;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class StreamContext extends AbstractReferenceCounted {
    private final EmbeddedChannel remuxer = new EmbeddedChannel(DefaultChannelId.newInstance());
    private final ChannelGroup rtmpGroup;
    private final ChannelGroup remuxGroup;
    private final String name;
    private final RtmpMessageToFrameHandler handler;
    private Channel publisher;
    private Message metaInit;
    private Message videoInit;
    private Message audioInit;

    public StreamContext(String name, EventExecutor executor) {
        this.name = name;
        this.rtmpGroup = new DefaultChannelGroup(executor);
        this.remuxGroup = new DefaultChannelGroup(executor);

        remuxer.pipeline().addLast(handler = new RtmpMessageToFrameHandler(remuxGroup, 500));
    }

    public String getName() {
        return name;
    }

    public Channel getPublisher() {
        return publisher;
    }

    public void setPublisher(Channel publisher) {
        if (publisher != null && this.publisher != null) {
            return;
        }
        if (publisher == null) {
            rtmpGroup.remove(remuxer);
            handler.cleanup();
            remuxGroup.close();
            endAll();
        } else {
            rtmpGroup.add(remuxer);
            beginAll(publisher.alloc());
        }
        this.publisher = publisher;
    }

    private void endAll() {
        rtmpGroup.writeAndFlush(streamEnd());
    }

    private Message streamEnd() {
        ByteBuf data = Unpooled.buffer();
        data.writeShort(1);
        data.writeInt(1);
        Message message = new Message(new Header(2, 0, Message.Type.USER, 0), data);
        data.release();
        return message;
    }

    public void beginAll(ByteBufAllocator alloc) {
        rtmpGroup.writeAndFlush(streamBegin(alloc));
        rtmpGroup.writeAndFlush(cmdPlayStart(alloc));
        rtmpGroup.writeAndFlush(cmdSampleAccess(alloc));
        rtmpGroup.writeAndFlush(cmdPublishNotify(alloc));
    }

    public void begin(Channel channel) {
        channel.writeAndFlush(streamBegin(channel.alloc()));
        channel.writeAndFlush(cmdPlayStart(channel.alloc()));
        channel.writeAndFlush(cmdSampleAccess(channel.alloc()));
        channel.writeAndFlush(cmdPublishNotify(channel.alloc()));
    }

    private Message cmdPublishNotify(ByteBufAllocator alloc) {
        ByteBuf data = alloc.buffer();
        Amf.serialize(data,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.PublishNotify",
                        "description", "Start publishing."
                )
        );
        Message message = new Message(new Header(5, 0, Message.Type.AMF0_CMD, 1), data);
        data.release();
        return message;
    }

    private Message cmdSampleAccess(ByteBufAllocator alloc) {
        ByteBuf data = alloc.buffer();
        Amf.serialize(data,
                "|RtmpSampleAccess",
                true,
                true
        );
        Message message = new Message(new Header(5, 0, Message.Type.AMF0_META, 1), data);
        data.release();
        return message;
    }

    private Message streamBegin(ByteBufAllocator alloc) {
        ByteBuf data = alloc.buffer();
        data.writeShort(0);
        data.writeInt(1);
        Message message = new Message(new Header(2, 0, Message.Type.USER, 0), data);
        data.release();
        return message;
    }

    private Message cmdPlayStart(ByteBufAllocator alloc) {
        ByteBuf data = alloc.buffer();
        Amf.serialize(data,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.Start",
                        "description", "Start live."
                )
        );
        Message message = new Message(new Header(5, 0, Message.Type.AMF0_CMD, 1), data);
        data.release();
        return message;
    }

    public void addRemuxSubscriber(final Channel channel) {
        ByteBuf init = handler.getInit();
        if (init != null) {
            init.retain();
            channel.writeAndFlush(init);
        }
        remuxGroup.add(channel);
        channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                remuxGroup.remove(channel); // TODO: do we need that?
            }
        });
    }

    public void addRtmpSubscriber(Channel channel) {
        if (publisher != null) {
            begin(channel);
        }
        if (metaInit != null && audioInit != null && videoInit != null) {
            channel.writeAndFlush(metaInit.retain());
            channel.writeAndFlush(audioInit.retain());
            channel.writeAndFlush(videoInit.retain());
        }
        rtmpGroup.add(channel);
    }

    public void removeRtmpSubscriber(Channel channel) {
        rtmpGroup.remove(channel);
    }

    public boolean containsRtmpSubscriber(Channel channel) {
        return rtmpGroup.contains(channel);
    }

    public int sizeRtmpSubscribers() {
        return rtmpGroup.size();
    }

    public void publish(Message message) {
        switch (message.getHeader().getType()) {
            case AMF0_META:
                if (metaInit == null) {
                    message.retain();
                    metaInit = message;
                }
                break;
            case AUDIO:
                if (audioInit == null) {
                    message.retain();
                    audioInit = message;
                }
                break;
            case VIDEO:
                if (videoInit == null) {
                    message.retain();
                    videoInit = message;
                }
                break;
            case USER:
                if (message.getData().getShort(0) == 1) {
                    if (metaInit != null) {
                        metaInit.release();
                        metaInit = null;
                    }
                    if (audioInit != null) {
                        audioInit.release();
                        audioInit = null;
                    }
                    if (videoInit != null) {
                        videoInit.release();
                        videoInit = null;
                    }
                }
                break;
        }
        rtmpGroup.writeAndFlush(message);
    }

    @Override
    protected void deallocate() {
        if (metaInit != null) {
            metaInit.release();
        }
        if (audioInit != null) {
            audioInit.release();
        }
        if (videoInit != null) {
            videoInit.release();
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}