package org.eientei.videostreamer.rtmp;

import io.netty.channel.socket.SocketChannel;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpClientContext implements RtmpClient {
    private final SocketChannel socket;

    public RtmpClientContext(SocketChannel socket) {
        this.socket = socket;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    @Override
    public void accept(RtmpMessage message) {
        if (message instanceof RtmpVideoMessage) {
            ((RtmpVideoMessage) message).getData();
        }
        socket.writeAndFlush(message);
    }

    public String getId() {
        return socket.id().asShortText();
    }
}
