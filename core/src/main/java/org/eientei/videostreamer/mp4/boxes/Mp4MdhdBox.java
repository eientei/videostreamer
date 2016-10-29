package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;
import org.eientei.videostreamer.mp4.Mp4Track;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MdhdBox extends Mp4BoxFull {
    private final Mp4Track track;

    public Mp4MdhdBox(Mp4RemuxerHandler context, Mp4Track track) {
        super("mdhd", context, 0, 0);
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(0); // creation time
        out.writeInt(0); // modification time
        out.writeInt((int)track.getTimescale()); // timescale
        out.writeInt(0); // duration
        out.writeShort(0x15C7); // language
        out.writeShort(0); // reserved
    }
}
