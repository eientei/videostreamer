package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.mp4.boxes.Mp4FtypBox;
import org.eientei.videostreamer.mp4.boxes.Mp4MoovBox;
import org.eientei.videostreamer.rtmp.RtmpSubscriber;
import org.eientei.videostreamer.util.AacHeader;
import org.eientei.videostreamer.util.PooledAllocator;
import org.eientei.videostreamer.ws.CommType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Context implements RtmpSubscriber {
    private final static int VIDEO_FORMAT_H264 = 7;
    private final static int AUDIO_FORMAT_MP3 = 2;
    private final static int AUDIO_FORMAT_AAC = 10;

    public final PooledAllocator ALLOC = new PooledAllocator();
    private final Logger log = LoggerFactory.getLogger(Mp4Context.class);
    //private final Set<Mp4Subscriber> subscribers = new ConcurrentSet<>();
    private AmfObjectWrapper metadata;
    private Mp4Track video;
    private Mp4Track audio;
    private List<Mp4Track> tracks = new ArrayList<>();
    private LinkedList<Mp4Frame> frames = new LinkedList<>();
    private Mp4Box[] header;
    private int moofid = 1;
    private volatile boolean inited = false;
    private final String name;
    private String codecs;
    private long time = System.currentTimeMillis();
    /*
    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);
    {
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (isReady(audio) && isReady(video)) {
                    try {
                        tryGetFrame(tracks);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }*/

    private Map<Mp4Subscriber, Mp4SubscriberContext> contexts = new HashMap<>();
    //private Map<Mp4Subscriber, Map<Mp4Track, Integer>> times = new ConcurrentHashMap<>();

    public Mp4Context(String name) {
        this.name = name;
    }

    private void addVideoTrack(ByteBuf videoro) {
        int fst = videoro.readByte();
        int frametype = (fst & 0xf0) >> 4;
        int videocodec = fst & 0x0f;
        int avcpacktype = videoro.readByte();
        int delay = videoro.readMedium();
        switch (videocodec) {
            case VIDEO_FORMAT_H264:
                video = new Mp4VideoTrackH264(this, metadata, videoro.slice());
                break;
        }

        if (video != null) {
            tracks.add(video);
        }
    }

    private void addAudioTrack(ByteBuf audioro) {
        int fst = audioro.readUnsignedByte();
        int audiocodec = (fst & 0xf0) >> 4;
        int channels = (fst & 0x01) + 1;
        int samplesiz = ((fst & 0x02) != 0) ? 2 : 1;
        int samplerate = (fst & 0x0c) >> 2;
        audioro.skipBytes(1);
        switch (audiocodec) {
            case AUDIO_FORMAT_AAC:
                AacHeader aacHeader = new AacHeader(audioro.slice());
                int sampleCount = (aacHeader.frameLenFlag == 1 ? 960 : 1024);
                audio = new Mp4AudioTrakAac(this, channels, samplesiz * 16, aacHeader.sampleRate, sampleCount, audioro.slice());
                break;
            case AUDIO_FORMAT_MP3:
                // TODO
                audio = new Mp4AudioTrakMp3(this, channels, samplerate, 0, audioro.slice());
                break;
        }

        if (audio != null) {
            tracks.add(audio);
        }
    }

    private Mp4Box[] makeHeader(List<Mp4Track> tracks) {
        Mp4FtypBox ftyp = new Mp4FtypBox(this, "mp42", 1, "mp42", "avc1", "iso5");
        Mp4MoovBox moov = new Mp4MoovBox(this, tracks);
        return new Mp4Box[] { ftyp, moov };
    }

    private void tryGetFrame(List<Mp4Track> tracks) {
        Mp4Frame frame = new Mp4Frame();
        for (Mp4Track track : tracks) {
            frame.append(track);
        }
        for (Map.Entry<Mp4Subscriber, Mp4SubscriberContext> subscriber : contexts.entrySet()) {
            if (subscriber.getValue().getTracktimes().get(tracks.get(0)) == null) {
                for (Mp4Track track : tracks) {
                    subscriber.getValue().getTracktimes().put(track, 0.0);
                    subscriber.getValue().setBegin(frame.getMinTimestamp(tracks));
                }
                subscriber.getKey().accept(tracks.get(0).getType(null), 0, 0, makeHeader(tracks));
            }

            CommType type;
            if (tracks.size() == 1) {
                type = tracks.get(0).getType(frame);
            } else {
                type = frame.isKeyframe() ? CommType.STREAM_UPDATE_AVK : CommType.STREAM_UPDATE_AV;
            }

            double min = frame.getMinTimestamp(tracks) - subscriber.getValue().getBegin();
            double max = frame.getMaxTimestamp(tracks) - subscriber.getValue().getBegin();
            subscriber.getKey().accept(type, (int)min, (int)max, frame.getMoof(this, tracks, subscriber.getValue()), frame.getMdat(this, tracks));
        }
        frame.release();
    }

    private boolean isReady(Mp4Track track) {
        return track == null || track.isSamplesReady();
        //boolean ready = true;
        //for (Mp4Track track : tracks) {
        //ready = ready && track.isSamplesReady();
        //}
        //return ready;
        //return true;
    }

    @Override
    public void acceptVideo(ByteBuf readonly, int timestamp) {
        if (video != null) {
            int fst = readonly.readByte();
            int frametype = (fst & 0xf0) >> 4;
            int videocodec = fst & 0x0f;
            int avcpacktype = readonly.readByte();
            int delay = readonly.readMedium();
            video.update(readonly, timestamp, frametype == 1);
            frameUpdate();
        }
        readonly.release();
    }

    @Override
    public void acceptAudio(ByteBuf readonly, int timestamp) {
        if (audio != null) {
            int fst = readonly.readUnsignedByte();
            int audiocodec = (fst & 0xf0) >> 4;
            int channels = (fst & 0x01) + 1;
            int samplesiz = ((fst & 0x02) != 0) ? 2 : 1;
            int samplerate = (fst & 0x0c) >> 2;
            readonly.skipBytes(1);
            audio.update(readonly, timestamp, true);
            frameUpdate();
        }
        readonly.release();
    }

    private void frameUpdate() {
        /*
        if (isReady(video)) {
            tryGetFrame(Collections.singletonList(video));
        }
        if (isReady(audio)) {
            tryGetFrame(Collections.singletonList(audio));
        }*/
        if (isReady(video) && isReady(audio) && System.currentTimeMillis() - time > 500) {
            time = System.currentTimeMillis();
            tryGetFrame(tracks);
        }
        //while (isReady(audio) && isReady(video)) {
            //List<Mp4Track> list = new ArrayList<>();
            //list.add(audio);
            //list.add(video);
            //tryGetFrame(list);
            //tryGetFrame(video);
            //tryGetFrame(audio);
            /*
            tryGetFrame(audio);
            tryGetFrame(video);
            */
        //}
    }

    @Override
    public void begin(AmfObjectWrapper metadata, ByteBuf videoro, ByteBuf audioro) {
        this.metadata = metadata;
        if (videoro.isReadable()) {
            addVideoTrack(videoro);
        }

        if (audioro.isReadable()) {
            addAudioTrack(audioro);
        }

        StringBuilder sb = new StringBuilder();
        if (video != null) {
            sb.append("avc1.42C01F");
        }
        if (audio != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("mp4a.40.2");
        }

        codecs = sb.toString();
        inited = true;

        for (Mp4Subscriber subscriber : contexts.keySet()) {
            subscriber.begin(codecs);
        }
    }

    @Override
    public void finish() {
        for (Map.Entry<Mp4Subscriber, Mp4SubscriberContext> subscriber : contexts.entrySet()) {
            subscriber.getKey().finish();
            subscriber.getValue().clear();
        }

        for (Mp4Track track : tracks) {
            track.release();
        }

        inited = false;
        tracks.clear();
        video = null;
        audio = null;
        header = null;
        metadata = null;
    }

    public void subscribe(Mp4Subscriber subscriber) {
        //for (Mp4Frame prevframe : frames) {
            //subscriber.accept(prevframe.getMoof(this, subscriber), prevframe.getMdat(this));
        //}
        contexts.put(subscriber, new Mp4SubscriberContext());

        if (inited) {
            subscriber.begin(codecs);
        }

        for (Mp4Subscriber s : contexts.keySet()) {
            s.count(contexts.size());
        }

        /*
        for (Mp4Track track : tracks) {
            if (frames.isEmpty()) {
                times.put(track.id(), 0);
            } else {
                times.put(track.id(), frames.getFirst().getBasetime(track.id()));
            }
        }

        */
    }

    public void unsubsribe(Mp4Subscriber subscriber) {
        subscriber.finish();
        contexts.remove(subscriber);
        for (Mp4Subscriber s : contexts.keySet()) {
            s.count(contexts.size());
        }
    }

    public List<Mp4Track> getTracks() {
        return tracks;
    }

    public AmfObjectWrapper getMetadata() {
        return metadata;
    }

    public int getNextMoofId() {
        return moofid++;
    }
}
