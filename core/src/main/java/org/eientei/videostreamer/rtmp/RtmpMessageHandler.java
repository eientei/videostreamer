package org.eientei.videostreamer.rtmp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.rtmp.message.RtmpCmdMessage;
import org.eientei.videostreamer.rtmp.message.RtmpSetChunkMessage;
import org.eientei.videostreamer.rtmp.message.RtmpSetPeerBandMessage;
import org.eientei.videostreamer.rtmp.message.RtmpWinackMessage;
import org.eientei.videostreamer.server.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class RtmpMessageHandler extends SimpleChannelInboundHandler<RtmpMessage> {
    private final Logger log = LoggerFactory.getLogger(RtmpDecoderHandler.class);
    private final ServerContext globalContext;

    public RtmpMessageHandler(ServerContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtmpMessage msg) throws Exception {
        switch (msg.getType()) {
            case SET_CHUNK_SIZE:
                ctx.pipeline().get(RtmpDecoderHandler.class).setChunkin(((RtmpSetChunkMessage)msg).getChunksize());
                break;
            case ACK:
                break;
            case USER:
                break;
            case WINACK:
                ctx.pipeline().get(RtmpDecoderHandler.class).setAckwindow(((RtmpWinackMessage)msg).getWinsize());
                break;
            case AMF3_META:
            case AMF0_META:
            case AUDIO:
            case VIDEO:
                msg.retain();
                ctx.fireChannelRead(msg);
                break;
            case AMF3_CMD_ALT:
            case AMF3_CMD:
            case AMF0_CMD:
                handleCmd(ctx, (RtmpCmdMessage)msg);
                break;
        }
    }

    private void handleCmd(ChannelHandlerContext ctx, RtmpCmdMessage msg) {
        AmfListWrapper amf = Amf.deserializeAll(msg.getData());
        String cmd = amf.get(0);
        log.info("{}: issued {}", this, cmd);

        switch (cmd) {
            case "connect":
                ctx.channel().writeAndFlush(new RtmpWinackMessage(2, 0, 0, ctx.alloc().buffer(), 5000000));
                ctx.channel().writeAndFlush(new RtmpSetPeerBandMessage(2, 0, 0, ctx.alloc().buffer(), 5000000, (byte) 2));
                ctx.channel().writeAndFlush(new RtmpSetChunkMessage(2, 0, 0, ctx.alloc().buffer(), 2048));
                ctx.pipeline().get(RtmpCodecHandler.class).setChunkout(2048);
                ctx.channel().writeAndFlush(new RtmpCmdMessage(3, 0, 0, ctx.alloc().buffer(),
                        "_result",
                        amf.get(1),
                        Amf.makeObject(
                                "fmsVer", "FMS/3,0,1,123",
                                "capabilities", 31.0
                        ),
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetConnection.Connect.Success",
                                "description", "Connection succeeded.",
                                "objectEncoding", 3.0
                        )
                ));
                break;
            case "createStream":
                ctx.writeAndFlush(new RtmpCmdMessage(3, 0, 0, ctx.alloc().buffer(),
                        "_result",
                        amf.get(1),
                        null,
                        1.0
                ));
                break;
            case "publish":
                String pubname = amf.get(3);
                if (!globalContext.publishRtmp(pubname, ctx.channel())) {
                    return;
                }
                ctx.channel().writeAndFlush(new RtmpCmdMessage(3, 0, 0, ctx.alloc().buffer(),
                        "onStatus",
                        0.0,
                        null,
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetStream.Publish.Start",
                                "description", "Start publising."
                        )
                ));
                break;
            case "play":
                String subname = amf.get(3);
                globalContext.subscribeRtmp(subname, ctx.channel());
                break;
        }
    }
}
