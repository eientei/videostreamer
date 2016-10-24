package org.eientei.videostreamer.ws;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.eientei.videostreamer.mp4.*;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class WebsocketCommContext implements Mp4Subscriber {
    private final WebSocketSession session;
    private final Mp4Server mp4Server;
    private Mp4Context context;

    public WebsocketCommContext(WebSocketSession session, Mp4Server mp4Server) {
        this.session = session;
        this.mp4Server = mp4Server;
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
    public void accept(Mp4Box... boxes) {
        ByteBuf buf = buffer(CommType.STREAM_UPDATE_AV);
        for (Mp4Box box : boxes) {
            box.write(buf);
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
