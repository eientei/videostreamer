package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.TrackType;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MoovBox extends Box {
    protected final MvhdBox mvhd;
    protected final MvexBox mvex;
    protected final TrakBox trak;

    public MoovBox(BoxContext context) {
        super("moov", context);
        mvhd = new MvhdBox(context);
        trak = new TrakBox(context);
        mvex = new MvexBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mvhd.write(out);
        if (context.get(Type.VIDEO_PRESENT)) {
            context.put(Type.TRACK_TYPE, TrackType.VIDEO);
            trak.write(out);
        }
        if (context.get(Type.AUDIO_PRESENT)) {
            context.put(Type.TRACK_TYPE, TrackType.AUDIO);
            trak.write(out);
        }
        mvex.write(out);
    }
}
