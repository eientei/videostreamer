package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.amf.AmfList;
import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.core.Header;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.exceptions.StreamAlreadyPublishingException;
import org.eientei.videostreamer.impl.exceptions.StreamAlreadySubscribedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alexander Tumin on 2016-11-04
 */
public class RtmpMessageHandler extends SimpleChannelInboundHandler<Message> {
    private Logger log = LoggerFactory.getLogger(RtmpMessageHandler.class);
    private final GlobalContext globalContext;

    public RtmpMessageHandler(GlobalContext globalContext) {

        this.globalContext = globalContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.getHeader().getType()) {
            case USER:
            case AUDIO:
            case VIDEO:
            case AMF3_META:
            case AMF0_META:
                msg.retain();
                ctx.fireChannelRead(msg);
                break;
            case AMF3_CMD_ALT:
            case AMF3_CMD:
            case AMF0_CMD:
                handleCmd(ctx, msg);
                break;
        }
    }

    private void handleCmd(ChannelHandlerContext ctx, Message msg) throws StreamAlreadyPublishingException, StreamAlreadySubscribedException {
        AmfList list = msg.asAmf();
        String cmd = list.getAs(0);
        log.info("{}: issued {}", this, cmd);
        switch (cmd) {
            case "connect":
                ByteBuf winack = ctx.alloc().buffer().writeInt(5000000);
                ctx.writeAndFlush(new Message(new Header(2, 0, Message.Type.WINACK, 0), winack));
                winack.release();
                ByteBuf setpeer = ctx.alloc().buffer().writeInt(5000000).writeByte(2);
                ctx.writeAndFlush(new Message(new Header(2, 0, Message.Type.SET_PEER_BAND, 0), setpeer));
                setpeer.release();
                ByteBuf setchunk = ctx.alloc().buffer().writeInt(2048);
                ctx.writeAndFlush(new Message(new Header(2, 0, Message.Type.SET_CHUNK_SIZE, 0), setchunk));
                setchunk.release();
                ByteBuf connect = ctx.alloc().buffer();
                Amf.serialize(connect,
                        "_result",
                        list.get(1),
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
                );
                ctx.writeAndFlush(new Message(new Header(3, 0, Message.Type.AMF0_CMD, 0), connect));
                connect.release();
                break;
            case "createStream":
                ByteBuf createStream = ctx.alloc().buffer();
                Amf.serialize(createStream,
                        "_result",
                        list.get(1),
                        null,
                        1.0
                );
                ctx.writeAndFlush(new Message(new Header(3, 0, Message.Type.AMF0_CMD, 0), createStream));
                createStream.release();
                break;
            case "publish":
                String pubname = list.getAs(3);
                globalContext.publish(pubname, ctx.channel());
                ByteBuf publish = ctx.alloc().buffer();
                Amf.serialize(publish,
                        "onStatus",
                        0.0,
                        null,
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetStream.Publish.Start",
                                "description", "Start publising."
                        )
                );
                ctx.writeAndFlush(new Message(new Header(3, 0, Message.Type.AMF0_CMD, 0), publish));
                publish.release();
                break;
            case "play":
                String subname = list.getAs(3);
                globalContext.subscribe(subname, ctx.channel());
                break;
        }
    }
}
