package org.eientei.videostreamer.rtmp;

import io.netty.channel.socket.SocketChannel;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpClientContext implements RtmpStreamClient {
    private final SocketChannel socket;
    private final RtmpServerContext context;
    private RtmpStream stream;
    private boolean bootstrapped = false;
    private boolean bootstrapping = false;

    public RtmpClientContext(SocketChannel socket, RtmpServerContext context) {
        this.socket = socket;
        this.context = context;
    }

    public void open() {
        context.connect(this);
    }

    public void close() {
        if (stream != null) {
            if (stream.getSource().equals(this)) {
                stream.setSource(null);
            } else {
                stream.getClients().remove(this);
            }
        }
        context.disconnect(this);
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public String getId() {
        return socket.id().asLongText();
    }

    public boolean publish(String streamName) {
        stream = context.acquireStream(streamName);
        if (stream.getSource() != null) {
            return false;
        }

        stream.setSource(this);
        return true;
    }

    public boolean play(String streamName) {
        stream = context.acquireStream(streamName);
        stream.getClients().add(this);
        return true;
    }

    public RtmpStream getStream() {
        return stream;
    }

    public synchronized void bootstrap() {
        bootstrapping = true;
        stream.bootstrap(this);
        bootstrapping = false;
        bootstrapped = true;
    }

    @Override
    public void accept(RtmpMessage message) {
        if (!bootstrapped && !bootstrapping) {
            return;
        }
        socket.writeAndFlush(message);
    }
}
