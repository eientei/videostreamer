package org.eientei.videostreamer.websock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.h264.SliceNalUnit;
import org.eientei.videostreamer.mp4.*;
import org.eientei.videostreamer.mp4.boxes.MdatBox;
import org.eientei.videostreamer.mp4.boxes.MoofBox;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageAcceptor;
import org.eientei.videostreamer.rtmp.RtmpStream;
import org.eientei.videostreamer.rtmp.message.RtmpAmfMessage;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpUserMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class WebsocketClient implements RtmpMessageAcceptor {
    private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);
    private final WebSocketSession session;
    private final RtmpStream rtmpStream;
    private final Mp4Context context = new Mp4Context();
    private Mp4VideoTrack video;
    private Mp4Track audio;
    private FileOutputStream fos;
    private boolean newclient = true;

    public WebsocketClient(WebSocketSession session, RtmpStream rtmpStream) {
        this.session = session;
        this.rtmpStream = rtmpStream;
        try {
            fos = new FileOutputStream(new File("dump.mp4"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public WebSocketSession getSession() {
        return session;
    }

    public RtmpStream getRtmpStream() {
        return rtmpStream;
    }

    @Override
    public void accept(RtmpMessage message) {
        if (message instanceof RtmpAmfMessage) {
            AmfListWrapper amf = ((RtmpAmfMessage) message).getAmf();
            if (amf.get(0).equals("onMetaData")) {
                Map<String, Object> map = amf.get(1);
                context.meta.framerate = ((Number)map.get("framerate")).intValue();
                context.meta.width = ((Number)map.get("width")).intValue();
                context.meta.height = ((Number)map.get("height")).intValue();
                log.info("{}x{} @ {}", context.meta.width, context.meta.height, context.meta.framerate);
            }
        } else if (message instanceof RtmpAudioMessage) {
            // ignore
        } else if (message instanceof RtmpVideoMessage) {
            ByteBuf data = message.getData();
            int fst = data.readUnsignedByte();
            int frametype = (fst & 0xf0) >> 4;
            int videocodec = fst & 0x0f;
            int avcpacktype = data.readUnsignedByte();
            int delay = data.readUnsignedMedium();
            if (avcpacktype == 0) {
                video = new Mp4VideoTrack(context, data);
                video.volume = 0;
                video.frametick = 1;
                video.timescale = context.meta.framerate;
                video.width = context.meta.width;
                video.height = context.meta.height;
            } else {
                if (newclient && frametype != 1) {
                 //   return;
                }

                newclient = false;

                if (context.sequence == 0) {
                    context.sequence++;
                    send(context.createHeader());
                }

                while (data.isReadable()) {
                    int size = data.readInt();
                    SliceNalUnit slice = new SliceNalUnit(video.sps, data, size);
                    ByteBuf naldata = data.copy(data.readerIndex(), size);
                    data.skipBytes(size);
                    video.addSample(new Mp4VideoSample(slice, naldata));
                    if (video.isCompleteFrame()) {
                        Mp4TrackFrame frame = video.getFrame();
                        MoofBox moof = new MoofBox(context, frame);
                        MdatBox mdat = new MdatBox(context, frame);
                        send(moof, mdat);
                        frame.dispose();
                    }
                }
            }
        } else if (message instanceof RtmpUserMessage) {
            if (((RtmpUserMessage) message).getEvent() == RtmpUserMessage.Event.STREAM_EOF) {
                try {
                    session.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        message.release();
    }

    private void send(Box... boxes) {
        ByteBuf out = Unpooled.buffer();
        for (Box box : boxes) {
            int before = out.writerIndex();
            box.write(out);
            int after = out.writerIndex();
            if (box instanceof MoofBox) {
                out.setInt(((MoofBox) box).frame.sizptr, after - before + 8);
            }
        }
        try {
            fos.write(out.array(), 0, out.readableBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            session.sendMessage(new BinaryMessage(out.array(), 0, out.readableBytes(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.release();
    }
}
