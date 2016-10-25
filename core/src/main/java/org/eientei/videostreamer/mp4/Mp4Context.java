package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ConcurrentSet;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4Context implements RtmpSubscriber {
    private final static int VIDEO_FORMAT_H264 = 7;
    private final static int AUDIO_FORMAT_MP3 = 2;
    private final static int AUDIO_FORMAT_AAC = 10;

    public final PooledAllocator ALLOC = new PooledAllocator();
    private final Logger log = LoggerFactory.getLogger(Mp4Context.class);
    private final Set<Mp4Subscriber> subscribers = new ConcurrentSet<>();
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

    private Map<Mp4Subscriber, Map<Mp4Track, Integer>> times = new ConcurrentHashMap<>();

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

    private synchronized void tryGetFrame() {
        Mp4Frame frame = new Mp4Frame();
        for (Mp4Track track : tracks) {
            frame.append(track);
        }
        for (Mp4Subscriber subscriber : subscribers) {
            if (!times.containsKey(subscriber)) {
                continue;
            }
            if (times.get(subscriber).isEmpty()) {
                for (Mp4Track track : tracks) {
                    times.get(subscriber).put(track, 0);
                }
                subscriber.accept(CommType.STREAM_UPDATE_AVK, makeHeader(tracks));
            }

            if (times.containsKey(subscriber)) {
                subscriber.accept(frame.isKeyframe() ? CommType.STREAM_UPDATE_AVK : CommType.STREAM_UPDATE_AV, frame.getMoof(this, tracks, times.get(subscriber)), frame.getMdat(this, tracks));
            }
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
    }

    @Override
    public void acceptVideo(ByteBuf readonly, int timestamp) {
        if (video != null) {
            int fst = readonly.readByte();
            int frametype = (fst & 0xf0) >> 4;
            int videocodec = fst & 0x0f;
            int avcpacktype = readonly.readByte();
            int delay = readonly.readMedium();
            video.update(readonly, frametype == 1);
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
            audio.update(readonly, true);
            frameUpdate();
        }
        readonly.release();
    }

    private void frameUpdate() {
        while (isReady(audio) && isReady(video)) {
            tryGetFrame();
            /*
            tryGetFrame(audio);
            tryGetFrame(video);
            */
        }
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

        for (Mp4Subscriber subscriber : subscribers) {
            subscriber.begin(codecs);
        }
    }

    @Override
    public void finish() {
        for (Mp4Subscriber subscriber : subscribers) {
            subscriber.finish();
            times.put(subscriber, new HashMap<Mp4Track, Integer>());
        }

        for (Mp4Track track : tracks) {
            track.release();
        }
        tracks.clear();
        video = null;
        audio = null;
        header = null;
        metadata = null;
    }

    public synchronized void subscribe(Mp4Subscriber subscriber) {
        //for (Mp4Frame prevframe : frames) {
            //subscriber.accept(prevframe.getMoof(this, subscriber), prevframe.getMdat(this));
        //}
        subscribers.add(subscriber);

        times.put(subscriber, new HashMap<Mp4Track, Integer>());

        if (inited) {
            subscriber.begin(codecs);
        }

        for (Mp4Subscriber s : subscribers) {
            s.count(subscribers.size());
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

    public synchronized void unsubsribe(Mp4Subscriber subscriber) {
        times.remove(subscriber);
        subscribers.remove(subscriber);
        for (Mp4Subscriber s : subscribers) {
            s.finish();
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
