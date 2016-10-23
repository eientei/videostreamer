package org.eientei.videostreamer.ws;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Server;
import org.eientei.videostreamer.mp4.Mp4Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class WebsocketLiveContext implements Mp4Subscriber {
    private final Logger log = LoggerFactory.getLogger(WebsocketLiveContext.class);
    private final Mp4Server mp4Server;
    private final JsonFactory jsonFactory = new JsonFactory();
    private FileOutputStream fis;
    private Mp4Context context;
    private WebSocketSession session;
    private Map<Integer, Integer> ticks = new HashMap<>();

    public WebsocketLiveContext(WebSocketSession session, Mp4Server mp4Server) {
        this.session = session;
        this.mp4Server = mp4Server;

        try {
            fis = new FileOutputStream(new File("dump.mp4"));
        } catch (FileNotFoundException e) {
        }
    }

    public void process(WebSocketSession session, TextMessage message) throws IOException {
        JsonParser parser = jsonFactory.createParser(message.getPayload());
        parser.nextToken();
        String action = parser.nextFieldName();
        String name = parser.nextTextValue();
        switch (action) {
            case "play":
                play(name);
                break;
        }
    }

    private void play(String name) throws IOException {
        context = mp4Server.getContext(name);
        if (context == null) {
            session.close();
            return;
        }
        context.subscribe(this);
    }

    @Override
    public void close() {
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
            }
        }
        if (context != null) {
            context.unsubsribe(this);
        }
    }

    @Override
    public void init(String codecs) {
        try {
            session.sendMessage(new TextMessage(codecs));
        } catch (IOException e) {
        }
    }

    @Override
    public void accept(Mp4Box... boxes) {
        ByteBuf buf = context.ALLOC.allocSizeless();
        for (Mp4Box box : boxes) {
            box.write(buf);
        }
        try {
            fis.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
            fis.flush();
            session.sendMessage(new BinaryMessage(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes(), true));
        } catch (IOException e) {
        }
        buf.release();
    }

    @Override
    public void addTick(int trackid, int amount) {
        if (!ticks.containsKey(trackid)) {
            ticks.put(trackid, 0);
        }
        ticks.put(trackid, ticks.get(trackid)+amount);
    }

    @Override
    public int getTick(int trackid) {
        if (!ticks.containsKey(trackid)) {
            return 0;
        }
        return ticks.get(trackid);
    }
}
