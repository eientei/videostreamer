package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.Mp4BoxFull;
import org.eientei.videostreamer.mp4.Mp4RemuxerHandler;
import org.eientei.videostreamer.mp4.Mp4Track;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
public class Mp4TfhdBox extends Mp4BoxFull {
    private final List<Mp4Track> tracks;
    private final Mp4Track track;

    public Mp4TfhdBox(Mp4RemuxerHandler context, List<Mp4Track> tracks, Mp4Track track) {
        super("tfhd", context, 0, 0x20 | 0x20000);
        this.tracks = tracks;
        this.track = track;
    }

    @Override
    protected void fullWrite(ByteBuf out) {
        out.writeInt(track.getId(tracks)); // track id
        out.writeInt(0x01010000); // sample flags, 0x20-controlled
    }
}
