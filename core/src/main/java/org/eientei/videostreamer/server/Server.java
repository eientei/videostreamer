package org.eientei.videostreamer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.eientei.videostreamer.conf.VideostreamerProperties;
import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.handlers.RtmpHandshakeHandler;
import org.eientei.videostreamer.impl.handlers.RtmpMessageDecoderHandler;
import org.eientei.videostreamer.impl.handlers.RtmpMessageEncoderHandler;
import org.eientei.videostreamer.impl.handlers.RtmpMessageHandler;
import org.eientei.videostreamer.impl.util.NamedThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
@Component
public class Server {
    private final EventLoopGroup masterGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-MASTER"));
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-WORKER"));

    @Autowired
    public Server(VideostreamerProperties properties, final GlobalContext globalContext) {
        if (properties.getRtmp().isEnabled()) {
            new ServerBootstrap()
                    .group(masterGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(final SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new RtmpHandshakeHandler(new Runnable() {
                                @Override
                                public void run() {
                                    socketChannel.pipeline().addLast(new RtmpMessageEncoderHandler());
                                    socketChannel.pipeline().addLast(new RtmpMessageDecoderHandler());
                                    socketChannel.pipeline().addLast(new RtmpMessageHandler(globalContext));
                                }
                            }));
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .bind(properties.getRtmp().getHost(), properties.getRtmp().getPort());
        }
    }
}
