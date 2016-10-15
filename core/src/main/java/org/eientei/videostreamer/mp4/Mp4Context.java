package org.eientei.videostreamer.mp4;

import org.eientei.videostreamer.mp4.boxes.FtypBox;
import org.eientei.videostreamer.mp4.boxes.MoovBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public class Mp4Context {
    public List<Mp4Track> tracks = new ArrayList<>();
    public Meta meta = new Meta();
    public int sequence;

    private FtypBox ftyp;
    private MoovBox moov;

    public static class Meta {
        public int framerate;
        public int width;
        public int height;
    }

    public Box[] createHeader() {
        if (ftyp == null) {
            ftyp = new FtypBox(this, "mp42", 1, "mp42", "avc1", "iso5");
        }
        if (moov == null) {
            moov = new MoovBox(this);
        }

        return new Box[] { ftyp, moov };
    }
}
