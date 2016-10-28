package org.eientei.videostreamer.ws;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.eientei.videostreamer.mp4.Mp4Box;
import org.eientei.videostreamer.mp4.Mp4Context;
import org.eientei.videostreamer.mp4.Mp4Server;
import org.eientei.videostreamer.mp4.Mp4Subscriber;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class WebsocketCommContext implements Mp4Subscriber {
    private final WebSocketSession session;
    private final Mp4Server mp4Server;
    private Mp4Context context;
    private FileOutputStream fosa;
    private FileOutputStream fosv;

    public WebsocketCommContext(WebSocketSession session, Mp4Server mp4Server) {
        this.session = session;
        this.mp4Server = mp4Server;
        try {
            fosa = new FileOutputStream(new File("fosa.mp4"));
            fosv = new FileOutputStream(new File("fosv.mp4"));
        } catch (Exception ignore) {
        }
    }

    public void process(String action, String params) {
        switch (action) {
            case "play":
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

    private synchronized void send(ByteBuf data) {
        try {
            session.sendMessage(new BinaryMessage(data.array(), data.arrayOffset() + data.readerIndex(), data.readableBytes(), true));
        } catch (IOException ignore) {
        } finally {
            data.release();
        }
    }

    private ByteBuf buffer(CommType type, int begin, int end) {
        ByteBuf buf = context.ALLOC.allocSizeless();
        buf.writeInt(type.getNum());
        buf.writeInt(begin);
        buf.writeInt(end);
        return buf;
    }

    public void close() {
        if (context != null) {
            context.unsubsribe(this);
        }
    }

    @Override
    public void begin(String codecs) {
        ByteBuf buf = buffer(CommType.STREAM_PLAY, 0, 0);
        buf.writeCharSequence(codecs, CharsetUtil.UTF_8);
        send(buf);
    }

    @Override
    public void accept(CommType type, int begin, int end, Mp4Box... boxes) {
        ByteBuf buf = buffer(type, begin, end);
        for (Mp4Box box : boxes) {
            box.write(buf);
        }

        try {
            switch (type) {
                case STREAM_UPDATE_A:
                case STREAM_UPDATE_AK:
                    fosa.write(buf.array(), buf.arrayOffset() + buf.readerIndex()+12, buf.readableBytes()-12);
                    break;
                case STREAM_UPDATE_V:
                case STREAM_UPDATE_VK:
                    fosv.write(buf.array(), buf.arrayOffset() + buf.readerIndex()+12, buf.readableBytes()-12);
                    break;
            }
        } catch (Exception ignore) {
        }
        send(buf);
    }

    @Override
    public void finish() {
        ByteBuf buf = buffer(CommType.STREAM_STOP, 0, 0);
        send(buf);
    }

    @Override
    public void count(int subscribers) {
        ByteBuf buf = buffer(CommType.STREAM_SUBSCRIBERS, 0, 0);
        buf.writeInt(subscribers);
        send(buf);
    }
}
