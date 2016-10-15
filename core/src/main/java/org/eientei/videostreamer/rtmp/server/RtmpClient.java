package org.eientei.videostreamer.rtmp.server;

import io.netty.channel.socket.SocketChannel;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageAcceptor;
import org.eientei.videostreamer.rtmp.RtmpStream;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpClient implements RtmpMessageAcceptor {
    private final RtmpServer server;
    private final SocketChannel socketChannel;
    private RtmpStream stream;

    public RtmpClient(RtmpServer server, SocketChannel socketChannel) {
        this.server = server;
        this.socketChannel = socketChannel;
    }

    public String getId() {
        return socketChannel.id().asShortText();
    }

    public RtmpStream getStream() {
        return stream;
    }
    public void setStream(RtmpStream stream) {
        this.stream = stream;
    }

    public void cleanup() {
        if (stream != null) {
            stream.cleanup(this);
        }
    }

    public RtmpStream acquireStream(String name) {
        return server.acquireStream(name);
    }

    public boolean isPublisher() {
        return stream.isPublisher(this);
    }

    @Override
    public synchronized void accept(RtmpMessage message) {
        if (socketChannel.isWritable()) {
            socketChannel.writeAndFlush(message).syncUninterruptibly();
        }
    }
}
