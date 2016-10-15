package org.eientei.videostreamer.rtmp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import org.eientei.videostreamer.config.VideostreamerProperties;
import org.eientei.videostreamer.config.rtmp.RtmpProperties;
import org.eientei.videostreamer.rtmp.RtmpStream;
import org.eientei.videostreamer.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Alexander Tumin on 2016-10-13
 */
@Component
public class RtmpServer {
    public final static AttributeKey<RtmpClient> CLIENT_CONTEXT = AttributeKey.valueOf("rtmp_client_context");

    private final Logger log = LoggerFactory.getLogger(RtmpServer.class);
    private final RtmpProperties rtmpProperties;
    private final Map<String, RtmpStream> streams = new HashMap<>();

    @Autowired
    public RtmpServer(VideostreamerProperties properties) {
        this.rtmpProperties = properties.getRtmp();
        log.info("RTMP server is starting");

        EventLoopGroup masterGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-MASTER"));
        EventLoopGroup slaveGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-WORKER"));

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(masterGroup, slaveGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.attr(CLIENT_CONTEXT).set(new RtmpClient(RtmpServer.this, socketChannel));
                            socketChannel.pipeline().addLast(new RtmpHandshakeHandler());
                            socketChannel.pipeline().addLast(new RtmpMessageCodec());
                            socketChannel.pipeline().addLast(new RtmpMessageDecoder());
                            socketChannel.pipeline().addLast(new RtmpMessageHandler());
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            b.bind(rtmpProperties.getHost(), rtmpProperties.getPort()).sync();
        } catch (InterruptedException e) {
            log.error("General network error", e);
        }
    }

    public synchronized RtmpStream acquireStream(String name) {
        RtmpStream stream = streams.get(name);
        if (stream == null) {
            stream = new RtmpStream(this, name);
            streams.put(name, stream);
        }
        return stream;
    }
}
