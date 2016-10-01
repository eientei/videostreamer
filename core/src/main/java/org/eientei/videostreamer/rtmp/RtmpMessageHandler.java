package org.eientei.videostreamer.rtmp;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpMessageHandler extends ChannelInboundHandlerAdapter {
    private Logger log = LoggerFactory.getLogger(RtmpMessageHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RtmpMessage message = (RtmpMessage) msg;

        if (message instanceof RtmpAmfCmdMessage) {
            handleCmd(ctx, (RtmpAmfCmdMessage)message);
        } else if (message instanceof RtmpAmfMetaMessage
                || message instanceof RtmpAudioMessage
                || message instanceof RtmpVideoMessage) {
            handleBroadcast(ctx, message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();
        RtmpStreamContext stream = ctx.channel().attr(RtmpServer.RTMP_STREAM_CONTEXT).get();
        if (stream != null) {
            stream.unpublish(client);
            stream.unsubscribe(client);
        }
        ctx.close();
        if (!(cause instanceof IOException)) {
            log.error("", cause);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();
        RtmpStreamContext stream = ctx.channel().attr(RtmpServer.RTMP_STREAM_CONTEXT).get();
        if (stream != null) {
            stream.unpublish(client);
            stream.unsubscribe(client);
        }
        log.info("Client {} disconnected", client.getId());
    }

    private void handleCmd(ChannelHandlerContext ctx, RtmpAmfCmdMessage message) throws Exception {
        RtmpServerContext server = ctx.channel().attr(RtmpServer.RTMP_SERVER_CONTEXT).get();
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();
        String name = (String) message.getValues().get(0);
        log.info("Client {} command {}", client.getId(), name);
        if (name.equals("connect")) {
            double serial = (double) message.getValues().get(1);
            client.accept(new RtmpWinackMessage(5000000));
            client.accept(new RtmpSetPeerBandMessage(5000000, 2));
            client.accept(new RtmpSetChunkSizeMessage(2048));

            List<Object> values = new ArrayList<>();
            values.add("_result");
            values.add(serial);
            values.add(Amf.makeObject(ImmutableMap.builder()
                    .put("fmsVer", "FMS/3,0,1,123")
                    .put("capabilities", 31.0)
                    .build()));
            values.add(Amf.makeObject(ImmutableMap.builder()
                    .put("level", "status")
                    .put("code", "NetConnection.Connect.Success")
                    .put("description", "Connection succeeded.")
                    .put("objectEncoding", 3.0)
                    .build()));
            RtmpAmf0CmdMessage cmd = new RtmpAmf0CmdMessage(values);
            cmd.getHeader().setChunkid(3);
            cmd.getHeader().setStreamid(0);
            client.accept(cmd);
        } else if (name.equals("createStream")) {
            double serial = (double) message.getValues().get(1);
            List<Object> values = new ArrayList<>();
            values.add("_result");
            values.add(serial);
            values.add(null);
            values.add(1.0);
            RtmpAmf0CmdMessage cmd = new RtmpAmf0CmdMessage(values);
            cmd.getHeader().setChunkid(3);
            cmd.getHeader().setStreamid(0);
            client.accept(cmd);
        } else if (name.equals("publish")) {
            String streamName = (String) message.getValues().get(3);
            RtmpStreamContext stream = server.getStream(streamName);
            ctx.channel().attr(RtmpServer.RTMP_STREAM_CONTEXT).set(stream);
            stream.publish(client);

            List<Object> values = new ArrayList<>();
            values.add("onStatus");
            values.add(0.0);
            values.add(null);
            values.add(Amf.makeObject(ImmutableMap.builder()
                    .put("level", "status")
                    .put("code", "NetStream.Publish.Start")
                    .put("description", "Start publising.")
                    .build()));
            RtmpAmf0CmdMessage cmd = new RtmpAmf0CmdMessage(values);
            cmd.getHeader().setChunkid(5);
            cmd.getHeader().setStreamid(0);
            client.accept(cmd);
        } else if (name.equals("play")) {
            String streamName = (String) message.getValues().get(3);
            RtmpStreamContext stream = server.getStream(streamName);
            ctx.channel().attr(RtmpServer.RTMP_STREAM_CONTEXT).set(stream);
            stream.subscribe(client);
        }
    }

    private void handleBroadcast(ChannelHandlerContext ctx, RtmpMessage message) {
        RtmpStreamContext stream = ctx.channel().attr(RtmpServer.RTMP_STREAM_CONTEXT).get();
        if (message instanceof  RtmpAmfMetaMessage) {
            stream.broadcastMetadata((RtmpAmfMetaMessage) message);
        } else if (message instanceof RtmpAudioMessage) {
            stream.broadcastAudio((RtmpAudioMessage) message);
        } else if (message instanceof RtmpVideoMessage) {
            stream.broadcastVideo((RtmpVideoMessage) message);
        }
    }
}
