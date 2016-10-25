package org.eientei.videostreamer.ws;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.eientei.videostreamer.mp4.*;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class WebsocketCommContext implements Mp4Subscriber {
    private final WebSocketSession session;
    private final Mp4Server mp4Server;
    private Mp4Context context;
    private FileOutputStream fos;

    public WebsocketCommContext(WebSocketSession session, Mp4Server mp4Server) {
        this.session = session;
        this.mp4Server = mp4Server;
        try {
            fos = new FileOutputStream(new File("dump.mp4"));
        } catch (FileNotFoundException ignore) {
        }
    }

    public void process(String action, String params) {
        switch (action) {
            case "play":
                if (context != null) {
                    context.unsubsribe(this);
                }
                context = mp4Server.getContext(params);
                context.subscribe(this);
                break;
            case "stop":
                if (context != null) {
                    context.unsubsribe(this);
                }
                break;
        }
    }

    private void send(ByteBuf data) {
        try {
            session.sendMessage(new BinaryMessage(data.array(), data.arrayOffset() + data.readerIndex(), data.readableBytes(), true));
        } catch (IOException ignore) {
        } finally {
            data.release();
        }
    }

    private ByteBuf buffer(CommType type) {
        ByteBuf buf = context.ALLOC.allocSizeless();
        buf.writeInt(type.getNum());
        return buf;
    }

    public void close() {
        if (context != null) {
            context.unsubsribe(this);
        }
    }

    @Override
    public void begin(String codecs) {
        ByteBuf buf = buffer(CommType.STREAM_PLAY);
        buf.writeCharSequence(codecs, CharsetUtil.UTF_8);
        send(buf);
    }

    @Override
    public void accept(CommType type, Mp4Box... boxes) {
        ByteBuf buf = buffer(type);
        for (Mp4Box box : boxes) {
            box.write(buf);
        }
        try {
            fos.write(buf.array(), buf.arrayOffset()+buf.readerIndex()+4, buf.readableBytes()-4);
        } catch (IOException ignore) {
        }
        send(buf);
    }

    @Override
    public void finish() {
        ByteBuf buf = buffer(CommType.STREAM_STOP);
        send(buf);
    }

    @Override
    public void count(int subscribers) {
        ByteBuf buf = buffer(CommType.STREAM_SUBSCRIBERS);
        buf.writeInt(subscribers);
        send(buf);
    }
}
