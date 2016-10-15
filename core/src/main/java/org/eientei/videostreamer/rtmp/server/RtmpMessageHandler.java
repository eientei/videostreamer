package org.eientei.videostreamer.rtmp.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageType;
import org.eientei.videostreamer.rtmp.RtmpStream;
import org.eientei.videostreamer.rtmp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpMessageHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(RtmpHandshakeHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RtmpMessage message = (RtmpMessage) msg;
        RtmpClient client = ctx.channel().attr(RtmpServer.CLIENT_CONTEXT).get();
        switch (message.getHeader().getType()) {
            case AMF3_META:
            case AMF0_META:
            case AUDIO:
            case VIDEO:
                if (client.isPublisher()) {
                    client.getStream().broadcast(message);
                }
                break;
            case AMF3_CMD_ALT:
            case AMF3_CMD:
            case AMF0_CMD:
                handleCommand(client, (RtmpAmfMessage)message);
                break;
        }

        message.release();
    }

    private void handleCommand(RtmpClient client, RtmpAmfMessage message) {
        AmfListWrapper amf = message.getAmf();
        String name = amf.get(0);
        log.info("{} issued a command: {}", client.getId(), name);

        switch (name) {
            case "connect": {
                double serial = amf.get(1);
                client.accept(new RtmpWinackMessage(2, 0, 0, 5000000));
                client.accept(new RtmpSetPeerBandMessage(2, 0, 0, 5000000, 2));
                client.accept(new RtmpSetChunkSizeMessage(2, 0, 0, 2048));
                client.accept(new RtmpAmfMessage(RtmpMessageType.AMF0_CMD, 3, 0, 0,
                        "_result",
                        serial,
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
            }
            case "createStream": {
                double serial = amf.get(1);
                client.accept(new RtmpAmfMessage(RtmpMessageType.AMF0_CMD, 3, 0, 0,
                        "_result",
                        serial,
                        null,
                        1.0
                ));
                break;
            }
            case "publish": {
                String streamName = amf.get(3);
                RtmpStream stream = client.acquireStream(streamName);
                stream.publish(client);
                client.accept(new RtmpAmfMessage(RtmpMessageType.AMF0_CMD, 3, 0, 0,
                        "onStatus",
                        0.0,
                        null,
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetStream.Publish.Start",
                                "description", "Start publising."
                        )
                ));
                client.setStream(stream);
                break;
            }
            case "play": {
                String streamName = amf.get(3);
                RtmpStream stream = client.acquireStream(streamName);
                stream.subscribe(client);
                client.setStream(stream);
                break;
            }
        }
    }
}