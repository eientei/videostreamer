package org.eientei.videostreamer.isoparser;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class WebsocketWritableByteChannel implements WritableByteChannel {
    private boolean closed = false;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final WebSocketSession session;

    public WebsocketWritableByteChannel(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        byte[] data = new byte[src.remaining()];
        src.get(data);
        baos.write(data);
        return data.length;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    public synchronized void flush() {
        try {
            System.out.println("send");
            baos.flush();
            session.sendMessage(new BinaryMessage(baos.toByteArray()));
        } catch (Exception e) {
        }
        baos.reset();
    }

    @Override
    public void close() throws IOException {
        flush();
        closed = true;
    }
}
