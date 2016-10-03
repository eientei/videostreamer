package org.eientei.videostreamer.html5;

import org.eientei.videostreamer.rtmp.RtmpClient;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Alexander Tumin on 2016-10-02
 */
public class Html5RtmpClient implements RtmpClient {
    private Logger log = LoggerFactory.getLogger(Html5RtmpClient.class);
    private final WebSocketSession session;

    public Html5RtmpClient(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public synchronized void accept(RtmpMessage message) {
        if (message instanceof RtmpVideoMessage) {
            //ByteBuffer buf = ByteBuffer.allocate(((RtmpVideoMessage) message).getData().length);
            //buf.put(0, (byte) message.getHeader().getType().getValue());
            //buf.put(((RtmpVideoMessage) message).getData());
            //buf.rewind();
            byte[] data = ((RtmpVideoMessage) message).getData();
            ByteBuffer wrap = ByteBuffer.wrap(data);
            int fmt = wrap.get();
            int frametype = (fmt & 0xf0) >> 4;
            int codecid = fmt & 0x0f;
            //log.info("type: {} codec: {}", frametype, codecid);
            int avcpacktype = wrap.get();
            //log.info("avcpacktype: {}", avcpacktype);
            int comptime = (wrap.getShort() << 8) | wrap.get();
            //log.info("comptme: {}", comptime);
            if (avcpacktype == 0) {
                int confver = wrap.get();
                //log.info("confver: {}", confver);
                int profile = wrap.get();
                //log.info("profile: {}", profile);
                int profile_comp = wrap.get();
                //log.info("profile_comp: {}", profile_comp);
                int avc_level = wrap.get();
                //log.info("avc_level: {}", avc_level);
                int nal_size = (wrap.get() & 0x03) + 1;
                //log.info("nal_size: {}", nal_size);

                int nal_sps_count = (wrap.get() & 0x1f);
                //log.info("nal_sps_count: {}", nal_sps_count);
                parses(wrap, nal_sps_count);
                int nal_pps_count = wrap.get();
                //log.info("nal_pps_count: {}", nal_pps_count);
                parses(wrap, nal_pps_count);
            } else if (avcpacktype == 1) {
                while (wrap.position() < wrap.capacity()) {
                    int nalu_size = wrap.getInt();
                    //log.info("nalu_size: {}", nalu_size);
                    byte[] nalu_cont = new byte[nalu_size];
                    wrap.get(nalu_cont);
                    //log.info("nalu_cont: {}", Arrays.toString(nalu_cont));
                    send(nalu_cont);
                }
            }
        }
    }

    private void parses(ByteBuffer wrap, int count) {
        for (int i = 0; i < count; i++) {
            int nal_len = wrap.getShort() & 0xFFFF;
            //log.info("  nal_len: {}", nal_len);
            byte[] nal_cont = new byte[nal_len];
            wrap.get(nal_cont);
            //log.info("  nal_cont: {}", Arrays.toString(nal_cont));
            send(nal_cont);
        }
    }

    private void send(byte[] nalu_cont) {
        try {
            session.sendMessage(new BinaryMessage(nalu_cont, true));
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
