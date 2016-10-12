package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;
import org.eientei.videostreamer.mp4.util.TrackType;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MoofBox extends Box {
    private final MfhdBox mfhd;
    private final TrafBox traf;

    public MoofBox(BoxContext context) {
        super("moof", context);
        mfhd = new MfhdBox(context);
        traf = new TrafBox(context);
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mfhd.write(out);
        if (context.get(Type.VIDEO_PRESENT)) {
            context.put(Type.TRACK_TYPE, TrackType.VIDEO);
            traf.write(out);
        }
        if (context.get(Type.AUDIO_PRESENT)) {
            context.put(Type.TRACK_TYPE, TrackType.AUDIO);
            traf.write(out);
        }
    }

    @Override
    protected void complete(ByteBuf out) {
        ByteBuf byteBuf = out.slice().resetReaderIndex();
        byteBuf.skipBytes(8); // moof len + 4CC
        byteBuf.skipBytes(byteBuf.getInt(byteBuf.readerIndex())); // mfhd skip
        byteBuf.skipBytes(8); // traf len + 4CC
        byteBuf.skipBytes(byteBuf.getInt(byteBuf.readerIndex())); // tfhd skip
        byteBuf.skipBytes(8); // trun len + 4CC
        byteBuf.skipBytes(4+4); // flags + sample count
        byteBuf.setInt(byteBuf.readerIndex(), out.readableBytes()+8); // moof offset
    }
}
