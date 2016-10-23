package org.eientei.videostreamer.rtmp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.eientei.videostreamer.conf.VideostreamerProperties;
import org.eientei.videostreamer.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
@Component
public class RtmpServer {
    private final Logger log = LoggerFactory.getLogger(RtmpServer.class);
    private final Map<String, RtmpStream> streams = new ConcurrentHashMap<>();

    private final EventLoopGroup masterGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-MASTER"));
    private final EventLoopGroup slaveGroup = new NioEventLoopGroup(0, new NamedThreadFactory("RTMP-WORKER"));

    private final ServerBootstrap bootstrap = new ServerBootstrap()
            .group(masterGroup, slaveGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    RtmpContext context = new RtmpContext(socketChannel, RtmpServer.this);
                    socketChannel.pipeline().addLast(new RtmpDecoderHandler(context));
                    socketChannel.pipeline().addLast(new RtmpCodecHandler(context));
                }
            })
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_BACKLOG, 128)
            .option(ChannelOption.SO_KEEPALIVE, true);

    private final List<RtmpPublishNotifier> notifiers;

    @Autowired
    public RtmpServer(VideostreamerProperties properties, List<RtmpPublishNotifier> notifiers) {
        this.notifiers = notifiers;
        if (properties.getRtmp().isEnabled()) {
            log.info("RTMP server is starting");
            bootstrap.bind(properties.getRtmp().getHost(), properties.getRtmp().getPort());
        }
    }

    public RtmpStream acquireStream(String name) {
        if (!streams.containsKey(name)) {
            streams.put(name, new RtmpStream(this, name));
        }
        return streams.get(name);
    }

    public List<RtmpPublishNotifier> getNotifiers() {
        return notifiers;
    }
}
