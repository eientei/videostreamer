package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.TrackType;

import java.util.Collections;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public abstract class Box {
    public enum Type {
        CREATION_TIME(0L),
        MODIFICATION_TIME(0L),
        FRAMERATE(0L),
        DURATION(0L),
        TRACK_ID(1),
        DEFAULT_SAMPLE_DESCRIPTION_INDEX(1),
        DEFAULT_SAMPLE_DURATION(1),
        DEFAULT_SAMPLE_SIZE(0),
        DEFAULT_SAMPLE_FLAGS(0),
        TRACK_TYPE(TrackType.VIDEO),
        WIDTH(0),
        HEIGHT(0),
        VIDEO_CODEC_ID(0),
        VIDEO_PRESENT(false),
        AUDIO_PRESENT(false),
        AUDIO_CODEC_ID(0),
        AUDIO_CHANNELS(2),
        AUDIO_SAMPLE_SIZE(16),
        AUDIO_SAMPLE_RATE(0),
        SEQUENCE_NUMBER(0),
        VIDEO_AVC_DATA(null),
        SAMPLES(Collections.emptyList()),
        BUFFER(1);
        /*
        MEDIA_DATA(null), // ByteBuf
        DELAY(0),
        IS_KEY(false)
         */

        private final Object val;

        Type(Object val) {
            this.val = val;
        }

        public Object getVal() {
            return val;
        }
    }

    protected final String atom;
    protected final BoxContext context;

    public Box(String atom, BoxContext context) {
        this.atom = atom;
        this.context = context;
    }

    public final void write(ByteBuf out) {
        int idx = out.writerIndex();
        out.writeInt(0);
        out.writeBytes(atom.getBytes());
        doWrite(out);
        out.setInt(idx, out.writerIndex() - idx);
        complete(out);
    }

    protected abstract void doWrite(ByteBuf out);

    protected void complete(ByteBuf out) {
        // to be overriden if needed
    }

    protected void writeMatrix(ByteBuf out, int a, int b, int c, int d, int tx, int ty) {
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

    protected <T> T getTrackTyped(T video, T audio) {
        switch ((TrackType)context.get(Type.TRACK_TYPE)) {
            case VIDEO:
                return video;
            case AUDIO:
                return audio;
        }
        return null;
    }
}
