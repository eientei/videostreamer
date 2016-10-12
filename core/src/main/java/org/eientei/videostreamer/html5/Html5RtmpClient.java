package org.eientei.videostreamer.html5;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.h264.SliceNalUnit;
import org.eientei.videostreamer.h264.SpsNalUnit;
import org.eientei.videostreamer.mp4.box.*;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.Sample;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eientei.videostreamer.mp4.box.Box.Type.*;

/**
 * Created by Alexander Tumin on 2016-10-02
 */
public class Html5RtmpClient implements RtmpClient {
    private Logger log = LoggerFactory.getLogger(Html5RtmpClient.class);
    private final WebSocketSession session;
    //private SpsNalUnit sps;
    //private PpsNalUnit pps;
    //private MetaData meta;
    //private int sequence = 0;
    private FileOutputStream fos;
    private static int[] sampleRates = new int[]{5512, 11025, 22050, 44100};
    //private Aac aac;
    private BoxContext context = new BoxContext();
    private SpsNalUnit sps;
    private long seq = 0;
    private long frameseq = -1;
    private long frame = 0;

    public Html5RtmpClient(WebSocketSession session) {
        this.session = session;
        try {
            fos = new FileOutputStream(new File("dump.mp4"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        context.put(SAMPLES, new ArrayList<>());
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
                context.put(FRAMERATE, (long)framerate);
                context.put(WIDTH, width);
                context.put(HEIGHT, height);
                context.put(BUFFER, framerate);
            }
        } else if (message instanceof RtmpAudioMessage) {
            ByteBuffer wrap = ByteBuffer.wrap(((RtmpAudioMessage) message).getData());
            int fmt = wrap.get(0);
            int audioCodecid = (fmt & 0xf0) >> 4;
            int audioChannels = (fmt & 0x01) + 1;
            int audioSampleSize = ((fmt & 0x2) != 0) ? 2 : 1;
            int audioSampleRate = sampleRates[(fmt & 0x0c) >> 2];

            context.put(AUDIO_CODEC_ID, audioCodecid);
            context.put(AUDIO_CHANNELS, audioChannels);
            context.put(AUDIO_SAMPLE_SIZE, audioSampleSize);
            context.put(AUDIO_SAMPLE_RATE, audioSampleRate);

        } else if (message instanceof RtmpVideoMessage) {
            byte[] data = ((RtmpVideoMessage) message).getData();
            ByteBuffer wrap = ByteBuffer.wrap(data);
            int fmt = wrap.get();
            int frametype = (fmt & 0xf0) >> 4;
            //context.put(IS_KEY, frametype == 1);
            int codecid = fmt & 0x0f;
            //log.info("type: {} codec: {}", frametype, codecid);
            int avcpacktype = wrap.get();
            //log.info("avcpacktype: {}", avcpacktype);
            int delay = (wrap.get() << 16) | (wrap.get() << 8) | wrap.get();
            //context.put(DELAY, delay);
            //int comptime = (wrap.getShort() << 8) | wrap.get();
            //log.info("comptme: {}", comptime);
            if (avcpacktype == 0) {
                byte[] avcC = new byte[wrap.remaining()];
                wrap.slice().get(avcC);
                context.put(VIDEO_PRESENT, true);
                context.put(VIDEO_CODEC_ID, codecid);
                context.put(VIDEO_AVC_DATA, Unpooled.wrappedBuffer(avcC));

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
                        sps = new SpsNalUnit(wrap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                int nal_pps_count = wrap.get();
                //log.info("nal_pps_count: {}", nal_pps_count);
                //processHeader(wrap, nal_pps_count, VIDEO_PPS_DATA);
            } else if (avcpacktype == 1) {
                if ((int)context.get(SEQUENCE_NUMBER) == 0) {
                    context.put(SEQUENCE_NUMBER, 0);
                    FtypBox ftyp = new FtypBox(context, "iso6", 1, "iso6");
                    MoovBox moov =new MoovBox(context);
                    send(ftyp, moov);
                    context.put(SEQUENCE_NUMBER, (int)context.get(SEQUENCE_NUMBER)+1);
                }

                while (wrap.remaining() > 0) {
                    int nalu_size = wrap.getInt(wrap.position())+4;

                    try {
                        SliceNalUnit slice = new SliceNalUnit(sps, (ByteBuffer) wrap.slice().position(4));

                        if (frameseq == -1) {
                            frametype = slice.frameNum;
                        }
                        byte[] mdatbuf = new byte[nalu_size];
                        wrap.get(mdatbuf);
                        if (slice.sliceType == 0) {
                            continue;
                        }

                        List<Sample> samples = context.get(SAMPLES);
                        samples.add(new Sample(mdatbuf, frametype == 1, delay, frameseq != slice.frameNum));
                        seq++;

                        if (frameseq != slice.frameNum) {
                            frameseq = slice.frameNum;
                            frame++;
                            if (frame >= (int)context.get(BUFFER)) {
                                frame = 0;
                                MoofBox moof = new MoofBox(context);
                                MdatBox mdat = new MdatBox(context);
                                send(moof, mdat);
                                context.put(SEQUENCE_NUMBER, (int) context.get(SEQUENCE_NUMBER) + 1);
                                samples.clear();
                            }
                            //context.put(MEDIA_DATA, Unpooled.wrappedBuffer(mdatbuf)); // TODO: verify no memory leak
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println(34);
            }
        }
    }

    private void processHeader(ByteBuffer wrap, int nal_sps_count, Box.Type type) {
        for (int i = 0; i < nal_sps_count; i++) {
            int nal_len = wrap.getShort() & 0xFFFF;
            byte[] mdatbuf = new byte[nal_len];
            wrap.get(mdatbuf);
            context.put(type, Unpooled.wrappedBuffer(mdatbuf));
        }
    }

    private synchronized void send(Box... boxes) {
        ByteBuf buf = Unpooled.buffer();
        for (Box box : boxes) {
            box.write(buf);
        }
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        try {
            fos.write(data);
            fos.flush();
            session.sendMessage(new BinaryMessage(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            fos.flush();
            fos.close();
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
