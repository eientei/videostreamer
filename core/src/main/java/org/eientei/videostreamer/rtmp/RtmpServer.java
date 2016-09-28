package org.eientei.videostreamer.rtmp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import org.eientei.videostreamer.config.VideostreamerProperties;
import org.eientei.videostreamer.config.rtmp.RtmpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
@Component
public class RtmpServer implements Runnable {
    private Logger log = LoggerFactory.getLogger(RtmpServer.class);
    private final RtmpProperties rtmp;
    public final static AttributeKey<RtmpClientContext> RTMP_CONNECTION_CONTEXT = AttributeKey.valueOf("rtmp_connection_context");
    public final static int RTMP_CHUNK_MAX = 65599;

    @Autowired
    private RtmpServerContext context;

    @Autowired
    public RtmpServer(VideostreamerProperties properties) {
        this.rtmp = properties.getRtmp();
    }

    @PostConstruct
    public void init() {
        if (rtmp.isEnabled()) {
            new Thread(this, "RTMP").start();
        }
    }

    public void run() {
        log.info("RTMP server launching");
        EventLoopGroup masterGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-MASTER"));
        EventLoopGroup slaveGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-WORKER"));
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(masterGroup, slaveGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            RtmpClientContext connectionContext = new RtmpClientContext(socketChannel, context);
                            socketChannel.attr(RTMP_CONNECTION_CONTEXT).set(connectionContext);
                            socketChannel.pipeline().addLast(new RtmpHandshakeHandler());
                            socketChannel.pipeline().addLast(new RtmpMessageCodec());
                            socketChannel.pipeline().addLast(new RtmpMessageDecoder());
                            socketChannel.pipeline().addLast(new RtmpMessageHandler());
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(rtmp
                    .getHost(), rtmp.getPort()).sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("General network error", e);
        } finally {
            masterGroup.shutdownGracefully();
            slaveGroup.shutdownGracefully();
        }
        log.info("RTMP server stopped");
    }
}
