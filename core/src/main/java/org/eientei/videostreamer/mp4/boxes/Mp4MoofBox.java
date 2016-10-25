package org.eientei.videostreamer.mp4.boxes;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4MoofBox extends Mp4Box {
    private final Mp4MfhdBox mfhd;
    private final List<Mp4TrafBox> trafs = new ArrayList<>();
    private final Map<Integer,Integer> offsets = new HashMap<>();

    public Mp4MoofBox(Mp4Context context, List<Mp4Track> tracks, Mp4Frame frame, Map<Mp4Track, Integer> times) {
        super("moof", context);
        this.mfhd = new Mp4MfhdBox(context);
        for (Mp4Track track : tracks) {
            if (frame.getSamples(track) != null) {
                trafs.add(new Mp4TrafBox(context, frame, track, times));
            }
        }
    }

    @Override
    protected void doWrite(ByteBuf out) {
        mfhd.write(out);
        offsets.clear();
        for (Mp4TrafBox traf : trafs) {
            traf.write(out);
            offsets.put(traf.getOffset(), traf.getCursiz());
        }
    }

    @Override
    protected void postprocess(ByteBuf out) {
        int offsiz = size + 8;
        for (Map.Entry<Integer, Integer> entry : offsets.entrySet()) {
            out.setInt(entry.getKey(), offsiz);
            offsiz += entry.getValue();
        }
    }
}
