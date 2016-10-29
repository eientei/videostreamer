package org.eientei.videostreamer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.eientei.videostreamer.conf.VideostreamerProperties;
import org.eientei.videostreamer.rtmp.RtmpHandshakeHandler;
import org.eientei.videostreamer.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
@Component
public class Server {
    private final Logger log = LoggerFactory.getLogger(Server.class);
    private final EventLoopGroup masterGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-MASTER"));
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-WORKER"));

    private final ServerContext globalContext;

    @Autowired
    public Server(final ServerContext globalContext, VideostreamerProperties properties) {
        this.globalContext = globalContext;
        if (properties.getRtmp().isEnabled()) {
            log.info("RTMP server is starting");
            final ChannelFuture f = new ServerBootstrap()
                    .group(masterGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new RtmpHandshakeHandler(globalContext));
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .bind(properties.getRtmp().getHost(), properties.getRtmp().getPort());
        }
    }
}
