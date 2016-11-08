package org.eientei.videostreamer.impl.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.impl.core.Frame;
import org.eientei.videostreamer.impl.core.Sample;
import org.eientei.videostreamer.impl.core.SampleList;
import org.eientei.videostreamer.impl.core.Track;
import org.eientei.videostreamer.impl.tracks.TrackAudio;
import org.eientei.videostreamer.impl.tracks.TrackVideo;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class Box {
    private static int beginBox(ByteBuf out, String boxname) {
        int idx = out.writerIndex();
        out.writeInt(0); // size
        out.writeBytes(boxname.getBytes());
        return idx;
    }

    private static int beginFullBox(ByteBuf out, String boxname, int version, int flags) {
        int idx = beginBox(out ,boxname);
        out.writeByte(version);
        out.writeMedium(flags);
        return idx;
    }

    private static void endBox(ByteBuf out, int idx) {
        out.setInt(idx, out.writerIndex() - idx);
    }

    private static void writeMatrix(ByteBuf out, int a, int b, int c, int d, int tx, int ty) {
        out.writeInt(a << 16);
        out.writeInt(b << 16);
        out.writeInt(0);

        out.writeInt(c << 16);
        out.writeInt(d << 16);
        out.writeInt(0);

        out.writeInt(tx << 16);
        out.writeInt(ty << 16);
        out.writeInt(1 << 30);
    }

    public static void ftyp(ByteBuf out) {
        int idx = beginBox(out, "ftyp");

        out.writeBytes("mp42".getBytes());
        out.writeInt(1);
        out.writeBytes("mp42".getBytes());
        out.writeBytes("avc1".getBytes());
        out.writeBytes("iso5".getBytes());

        endBox(out, idx);
    }

    public static void moov(ByteBuf out, Track audio, Track video) {
        int idx = beginBox(out, "moov");

        mvhd(out);
        trak(out, audio, 1);
        trak(out, video, 2);
        mvex(out);

        endBox(out, idx);
    }

    public static int[] moof(ByteBuf out, Frame frame) {
        int idx = beginBox(out, "moof");

        mfhd(out, frame.getSequence());
        int[] audioOffs = traf(out, frame.getAudioList(), 1);
        int[] videoOffs = traf(out, frame.getVideoList(), 2);

        endBox(out, idx);

        out.setInt(audioOffs[1], out.writerIndex() - idx + 8);
        out.setInt(videoOffs[1], out.writerIndex() - idx + 8 + frame.getAudioList().getTotalSize());

        return new int[]{ audioOffs[0], videoOffs[0] };
    }

    public static void mdat(ByteBuf out, Frame frame) {
        int idx = beginBox(out, "mdat");

        for (Sample sample : frame.getAudioList().getSamples()) {
            out.writeBytes(sample.getData());
        }

        for (Sample sample : frame.getVideoList().getSamples()) {
            out.writeBytes(sample.getData());
        }

        endBox(out, idx);
    }

    private static int[] traf(ByteBuf out, SampleList samples, int id) {
        int idx = beginBox(out, "traf");
        int[] rets = new int[2];

        tfhd(out, id);
        rets[0] = tfdt(out);
        rets[1] = trun(out, samples);

        endBox(out, idx);

        return rets;
    }

    private static int trun(ByteBuf out, SampleList samples) {
        int idx = beginFullBox(out, "trun", 0, 0x01 | 0x04 | 0x100 | 0x0200);

        out.writeInt(samples.getSamples().size());
        int dataOffset = out.writerIndex();
        out.writeInt(0); // data offset, post-baked
        out.writeInt(0x2000000); // flags
        for (Sample sample : samples.getSamples()) {
            out.writeInt(samples.getFrametick()); // duration
            out.writeInt(sample.getData().readableBytes()); // size
        }

        endBox(out, idx);

        return dataOffset;
    }

    private static int tfdt(ByteBuf out) {
        int idx = beginFullBox(out, "tfdt", 0, 0);

        int timeOffset = out.writerIndex();
        out.writeInt(0);

        endBox(out, idx);

        return timeOffset;
    }

    private static void tfhd(ByteBuf out, int id) {
        int idx = beginFullBox(out, "tfhd", 0, 0x20 | 0x20000);

        out.writeInt(id); // track id
        out.writeInt(0x01010000); // sample flags, 0x20-controlled

        endBox(out, idx);
    }


    private static void mfhd(ByteBuf out, int sequence) {
        int idx = beginFullBox(out, "mfhd", 0, 0);

        out.writeInt(sequence);

        endBox(out, idx);
    }

    private static void mvex(ByteBuf out) {
        int idx = beginBox(out, "mvex");

        trex(out, 1);
        trex(out, 2);

        endBox(out, idx);
    }

    private static void trex(ByteBuf out, int id) {
        int idx = beginFullBox(out, "trex", 0, 0);

        out.writeInt(id); // trak id
        out.writeInt(1); // default sample description index
        out.writeInt(0); // default sample duration
        out.writeInt(0); // default sample size
        out.writeInt(0); // default sample flags

        endBox(out, idx);
    }

    private static void trak(ByteBuf out, Track track, int id) {
        int idx = beginBox(out, "trak");

        tkhd(out, track, id);
        mdia(out, track, id);

        endBox(out, idx);
    }

    private static void mdia(ByteBuf out, Track track, int id) {
        int idx = beginBox(out, "mdia");

        mdhd(out, track);
        hdlr(out, track);
        minf(out, track, id);

        endBox(out, idx);
    }

    private static void minf(ByteBuf out, Track track, int id) {
        int idx = beginBox(out, "minf");

        if (track instanceof TrackAudio) {
            smhd(out);
        } else if (track instanceof TrackVideo) {
            vmhd(out);
        }

        dinf(out);
        stbl(out, track, id);

        endBox(out, idx);
    }

    private static void stbl(ByteBuf out, Track track, int id) {
        int idx = beginBox(out, "stbl");

        stsd(out, track, id);
        stsz(out);
        stsc(out);
        stts(out);
        stco(out);

        endBox(out, idx);
    }

    private static void stco(ByteBuf out) {
        int idx = beginFullBox(out, "stco", 0, 0);

        out.writeInt(0); // entry count

        endBox(out, idx);
    }

    private static void stts(ByteBuf out) {
        int idx = beginFullBox(out, "stts", 0, 0);

        out.writeInt(0); // entry count

        endBox(out, idx);
    }

    private static void stsc(ByteBuf out) {
        int idx = beginFullBox(out, "stsc", 0, 0);

        out.writeInt(0); // entry count

        endBox(out, idx);
    }

    private static void stsz(ByteBuf out) {
        int idx = beginFullBox(out, "stsz", 0, 0);

        out.writeInt(0); // sample size
        out.writeInt(0); // sample countx

        endBox(out, idx);
    }

    private static void stsd(ByteBuf out, Track track, int id) {
        int idx = beginFullBox(out, "stsd", 0, 0);

        out.writeInt(1); // entry count
        if (track instanceof TrackAudio) {
            mp4a(out, track, id);
        } else if (track instanceof TrackVideo) {
            avc1(out, track);
        }

        endBox(out, idx);
    }

    private static void avc1(ByteBuf out, Track track) {
        int idx = beginBox(out, "avc1");

        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeShort(1);

        out.writeShort(0);
        out.writeShort(0);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(((TrackVideo)track).getWidth());
        out.writeShort(((TrackVideo)track).getHeight());

        out.writeInt(0x00480000);
        out.writeInt(0x00480000);
        out.writeInt(0);
        out.writeShort(1);

        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(0x0018);
        out.writeShort(-1);

        avcC(out, (TrackVideo) track);

        endBox(out, idx);
    }

    private static void avcC(ByteBuf out, TrackVideo track) {
        int idx = beginBox(out, "avcC");

        out.writeBytes(track.getInit());

        endBox(out, idx);
    }

    private static void mp4a(ByteBuf out, Track track, int id) {
        int idx = beginBox(out, "mp4a");

        out.writeInt(0);
        out.writeShort(0);

        out.writeShort(1);

        out.writeInt(0);
        out.writeInt(0);

        out.writeShort(((TrackAudio)track).getChannels());
        out.writeShort(((TrackAudio)track).getSampleSize());

        out.writeInt(0);
        out.writeShort(track.getTimescale());
        out.writeShort(((TrackAudio)track).getSampleRate());
        esds(out, (TrackAudio) track, id);

        endBox(out, idx);
    }

    private static void esds(ByteBuf out, TrackAudio track, int id) {
        int idx = beginFullBox(out, "esds", 0, 0);

        out.writeByte(0x03);
        out.writeByte(23+track.getInit().readableBytes());
        out.writeShort(id);
        out.writeByte(0);

        out.writeByte(0x04);
        out.writeByte(15+track.getInit().readableBytes());
        out.writeByte(0x40);
        out.writeByte(0x15);
        out.writeMedium(0);
        out.writeInt(128000);
        out.writeInt(128000);

        out.writeByte(0x05);
        out.writeByte(track.getInit().readableBytes());
        out.writeBytes(track.getInit());

        out.writeByte(0x06);
        out.writeByte(1);
        out.writeByte(0x02);

        endBox(out, idx);
    }

    private static void dinf(ByteBuf out) {
        int idx = beginBox(out, "dinf");

        dref(out);

        endBox(out, idx);
    }

    private static void dref(ByteBuf out) {
        int idx = beginFullBox(out, "dref", 0, 0);

        out.writeInt(1);
        url_(out);

        endBox(out, idx);
    }

    private static void url_(ByteBuf out) {
        int idx = beginFullBox(out, "url ", 0, 0x01);
        endBox(out, idx);
    }

    private static void vmhd(ByteBuf out) {
        int idx = beginFullBox(out, "vmhd", 0, 0x01);

        out.writeShort(0); // graphic mode
        out.writeShort(0); // red
        out.writeShort(0); // green
        out.writeShort(0); // blue

        endBox(out, idx);
    }

    private static void smhd(ByteBuf out) {
        int idx = beginFullBox(out, "smhd", 0, 0);

        out.writeShort(0); // balance
        out.writeShort(0); // reserved

        endBox(out, idx);
    }

    private static void hdlr(ByteBuf out, Track track) {
        int idx = beginFullBox(out, "hdlr", 0, 0);

        String shortHandler = "";
        String longHandler = "";
        if (track instanceof TrackAudio) {
            shortHandler = "soun";
            longHandler = "Sound Handler";
        } else if (track instanceof TrackVideo) {
            shortHandler = "vide";
            longHandler = "Video Handler";
        }

        out.writeInt(0); // predefined
        out.writeBytes(shortHandler.getBytes()); // handler type
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeBytes(longHandler.getBytes()); // name
        out.writeByte(0); // null-terminator

        endBox(out, idx);
    }

    private static void mdhd(ByteBuf out, Track track) {
        int idx = beginFullBox(out, "mdhd", 0, 0);

        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(track.getTimescale()); // timescale
        out.writeInt(0); // duration
        out.writeShort(0x15C7); // language
        out.writeShort(0); // reserved

        endBox(out, idx);
    }

    private static void tkhd(ByteBuf out, Track track, int id) {
        int idx = beginFullBox(out, "tkhd", 0, 0x07);

        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(id); // track id
        out.writeInt(0); // reserved
        out.writeInt(0); // duration

        out.writeInt(0); // reserved
        out.writeInt(0); // reserved

        out.writeShort(0); // layer
        out.writeShort(0); // alternate group
        out.writeShort(1); // volume
        out.writeShort(0); // reserved

        writeMatrix(out, 1, 0, 0, 1, 0, 0); // unity matrix

        if (track instanceof TrackVideo) {
            out.writeInt(((TrackVideo) track).getWidth() << 16); // width
            out.writeInt(((TrackVideo) track).getHeight() << 16); // height
        } else {
            out.writeInt(0); // width
            out.writeInt(0); // height
        }

        endBox(out, idx);
    }

    private static void mvhd(ByteBuf out) {
        int idx = beginFullBox(out, "mvhd", 0, 0);

        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt(1000); // timescale
        out.writeInt(0); // duration

        out.writeInt(0x00010000); // rate
        out.writeShort(0x0100); // volume
        out.writeShort(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        writeMatrix(out, 1, 0, 0, 1, 0, 0); // unity matrix

        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined
        out.writeInt(0); // predefined

        out.writeInt(3); // next track id

        endBox(out, idx);
    }
}
