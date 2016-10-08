package org.eientei.videostreamer.html5;

import org.eientei.videostreamer.aac.Aac;
import org.eientei.videostreamer.h264.PpsNalUnit;
import org.eientei.videostreamer.h264.SliceNalUnit;
import org.eientei.videostreamer.h264.SpsNalUnit;
import org.eientei.videostreamer.mp4.*;
import org.eientei.videostreamer.rtmp.RtmpClient;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.message.RtmpAmfMetaMessage;
import org.eientei.videostreamer.rtmp.message.RtmpAudioMessage;
import org.eientei.videostreamer.rtmp.message.RtmpUserMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-02
 */
public class Html5RtmpClient implements RtmpClient {
    private Logger log = LoggerFactory.getLogger(Html5RtmpClient.class);
    private final WebSocketSession session;
    private SpsNalUnit sps;
    private PpsNalUnit pps;
    private MetaData meta;
    private int sequence = 0;
    private FileOutputStream fos;
    private static int[] sampleRates = new int[]{5512, 11025, 22050, 44100};
    private Aac aac;

    public Html5RtmpClient(WebSocketSession session) {
        this.session = session;
        try {
            fos = new FileOutputStream(new File("dump.mp4"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void accept(RtmpMessage message) {
        if (message instanceof RtmpUserMessage) {
        } else if (message instanceof RtmpAmfMetaMessage) {
            List<Object> values = ((RtmpAmfMetaMessage) message).getValues();
            if ("onMetaData".equals(values.get(0))) {
                Map<String, Object> map = (Map<String, Object>) values.get(1);
                int framerate = ((Double) map.get("framerate")).intValue();
                int width = ((Double) map.get("width")).intValue();
                int height = ((Double) map.get("height")).intValue();
                meta = new MetaData(framerate, width, height, (int) message.getHeader().getTimestamp());
            }
        } else if (message instanceof RtmpAudioMessage) {
            ByteBuffer wrap = ByteBuffer.wrap(((RtmpAudioMessage) message).getData());
            int fmt = wrap.get(0);
            int audioCodecid = (fmt & 0xf0) >> 4;
            int audioChannels = (fmt & 0x01) + 1;
            int audioSampleSize = ((fmt & 0x2) != 0) ? 2 : 1;
            int audioSampleRate = sampleRates[(fmt & 0x0c) >> 2];

            meta.setAudioCodecId(audioCodecid);
            meta.setAudioChannels(audioChannels);
            meta.setAudioSampleSize(audioSampleSize);
            meta.setAudioSampleRate(audioSampleRate);

            if (audioCodecid == 10) {
                try {
                    aac = new Aac(wrap);
                    meta.setAudioDsi(aac.buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if (message instanceof RtmpVideoMessage) {
            byte[] data = ((RtmpVideoMessage) message).getData();
            ByteBuffer wrap = ByteBuffer.wrap(data);
            int fmt = wrap.get();
            int frametype = (fmt & 0xf0) >> 4;
            int codecid = fmt & 0x0f;
            meta.setVideoCodecId(codecid);
            //log.info("type: {} codec: {}", frametype, codecid);
            int avcpacktype = wrap.get();
            //log.info("avcpacktype: {}", avcpacktype);
            int comptime = (wrap.getShort() << 8) | wrap.get();
            //log.info("comptme: {}", comptime);
            if (avcpacktype == 0) {
                byte[] avcC = new byte[wrap.remaining()];
                wrap.slice().get(avcC);
                meta.setAvcc(avcC);

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
                for (int i = 0; i < nal_sps_count; i++) {

                    int nal_len = wrap.getShort() & 0xFFFF;
                    try {
                        sps = new SpsNalUnit((ByteBuffer) wrap.slice().limit(nal_len));
                        meta.setTimeScale(1000);
                        if (sps.vui != null) {
                            meta.setTimeScale(sps.vui.timeScale);
                            meta.setFrameTick(sps.vui.numUnitsInTick);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] mdatbuf = new byte[nal_len];
                    wrap.get(mdatbuf);
                    /*
                    MoofBox moof = new MoofBox(meta, sequence++, Collections.singletonList(new Sample(sps, mdatbuf.length)));
                    MdatBox mdat = new MdatBox(mdatbuf);
                    send(moof, mdat);
                    */
                }
                int nal_pps_count = wrap.get();
                //log.info("nal_pps_count: {}", nal_pps_count);
                for (int i = 0; i < nal_pps_count; i++) {
                    int nal_len = wrap.getShort() & 0xFFFF;
                    byte[] mdatbuf = new byte[nal_len];
                    wrap.get(mdatbuf);

                    /*
                    MoofBox moof = new MoofBox(meta, sequence++, Collections.singletonList(new Sample(sps, mdatbuf.length)));
                    MdatBox mdat = new MdatBox(mdatbuf);
                    send(moof, mdat);
                    */
                }
            } else if (avcpacktype == 1) {
                sequence++;
                if (sequence == 1) {
                    send(new FtypBox());
                    send(new MoovBox(meta));
                }
                while (wrap.remaining() > 0) {
                    int nalu_size = wrap.getInt(wrap.position())+4;
                    log.info("nalu_size: {}", nalu_size);
                    try {
                        SliceNalUnit slice = new SliceNalUnit(sps, (ByteBuffer) wrap.slice().position(4));
                        if (slice.sliceType == 0) {
                            wrap.position(wrap.position()+nalu_size);
                            continue;
                        }
                        log.info("{} {}", slice.sliceType, slice.frameNum);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] mdatbuf = new byte[nalu_size];
                    wrap.get(mdatbuf);
                    MoofBox moof = new MoofBox(meta, sequence, Collections.singletonList(new Sample(meta, mdatbuf.length)));
                    MdatBox mdat = new MdatBox(mdatbuf);
                    send(moof, mdat);
                }
            } else {
                System.out.println(34);
            }
        }
    }

    private void send(Box... boxes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Box box : boxes) {
            try {
                baos.write(box.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fos.write(baos.toByteArray());
            fos.flush();
            session.sendMessage(new BinaryMessage(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

    }
}
