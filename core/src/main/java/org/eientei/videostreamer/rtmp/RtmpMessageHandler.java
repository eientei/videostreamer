package org.eientei.videostreamer.rtmp;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.rtmp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpMessageHandler extends ChannelInboundHandlerAdapter {
    private Logger log = LoggerFactory.getLogger(RtmpMessageHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RtmpMessage)) {
            return;
        }
        RtmpClientContext connctx = ctx.channel().attr(RtmpServer.RTMP_CONNECTION_CONTEXT).get();
        RtmpMessage message = (RtmpMessage) msg;
        if (message instanceof RtmpAmfCmdMessage) {
            handleAmfCmd(connctx, (RtmpAmfCmdMessage)msg);
        } else if (message instanceof RtmpAmfMetaMessage) {
            handleBroadcast(connctx, message);
        } else if (message instanceof RtmpUserMessage) {
            handleUser(connctx, (RtmpUserMessage)message);
        } else if (message instanceof RtmpAudioMessage) {
            handleBroadcast(connctx, message);
        } else if (message instanceof RtmpVideoMessage) {
            handleBroadcast(connctx, message);
        }
    }

    private void handleBroadcast(RtmpClientContext connctx, RtmpMessage message) {
        connctx.getStream().broadcast(message);
    }

    private void handleUser(RtmpClientContext connctx, RtmpUserMessage message) {

    }


    private void handleAmfCmd(RtmpClientContext connctx, RtmpAmfCmdMessage msg) {
        String name = (String) msg.getValues().get(0);
        if (name.equals("connect")) {
            double serial = (double) msg.getValues().get(1);
            connctx.getSocket().writeAndFlush(new RtmpWinackMessage(5000000));
            connctx.getSocket().writeAndFlush(new RtmpSetPeerBandMessage(5000000, 2));
            connctx.getSocket().writeAndFlush(new RtmpSetChunkSizeMessage(4096));

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
            connctx.getSocket().writeAndFlush(new RtmpAmf0CmdMessage(values));
        } else if (name.equals("publish")) {
            String streamName = (String) msg.getValues().get(3);
            if (!connctx.publish(streamName)) {
                connctx.getSocket().close();
                return;
            }

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
            //cmd.getHeader().setChunkid(5);
            //cmd.getHeader().setStreamid(1); // ?
            connctx.getSocket().writeAndFlush(cmd);
        } else if (name.equals("play")) {
            String streamName = (String) msg.getValues().get(3);
            if (!connctx.play(streamName)) {
                return;
            }
            List<Object> values = new ArrayList<>();
            values.add("onStatus");
            values.add(0.0);
            values.add(null);
            values.add(Amf.makeObject(ImmutableMap.builder()
                    .put("level", "status")
                    .put("code", "NetStream.Play.Start")
                    .put("description", "Start live.")
                    .build()));
            RtmpAmf0CmdMessage cmd = new RtmpAmf0CmdMessage(values);
            //cmd.getHeader().setChunkid(5);
            //cmd.getHeader().setStreamid(1);
            connctx.getSocket().writeAndFlush(cmd);

            List<Object> metavalues = new ArrayList<>();
            metavalues.add("|RtmpSampleAccess");
            metavalues.add(true);
            metavalues.add(true);
            RtmpAmfMetaMessage meta = new RtmpAmfMetaMessage(metavalues);
            //meta.getHeader().setChunkid(5);
            //meta.getHeader().setChunkid(1);
            connctx.getSocket().writeAndFlush(meta);

            connctx.bootstrap();
        } else if (name.equals("createStream")) {
            double serial = (double) msg.getValues().get(1);
            List<Object> values = new ArrayList<>();
            values.add("_result");
            values.add(serial);
            values.add(null);
            values.add(1.0);
            connctx.getSocket().writeAndFlush(new RtmpAmf0CmdMessage(values));
        }
    }
}
