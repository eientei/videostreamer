package org.eientei.videostreamer.web;

import org.eientei.videostreamer.websock.WebsocketClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Alexander Tumin on 2016-10-18
 */
public class PseudoInputStream extends InputStream {
    private final WebsocketClient client;

    public PseudoInputStream(WebsocketClient client) {
        this.client = client;
    }

    @Override
    public int read() throws IOException {
        while (!client.baos.isReadable()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return client.baos.readByte();
    }

    @Override
    public void close() throws IOException {
        client.baos.release();
        client.getRtmpStream().cleanup(client);
    }
}
