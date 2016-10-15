package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-14
 */
public abstract class Mp4Track {
    protected final Mp4Context context;
    public int volume;
    public int width;
    public int height;
    public int timescale;
    public int frametick;
    public Box init;
    public byte[] shorthandler;
    public byte[] longhandler;
    public Box mhd;

    public Mp4Track(Mp4Context context, String shorth, String longh, Box mhd) {
        this.context = context;
        shorthandler = shorth.getBytes();
        longhandler = longh.getBytes();
        this.mhd = mhd;
        context.tracks.add(this);
    }

    public abstract Mp4TrackFrame getFrame();

    public int idx() {
        return context.tracks.indexOf(this) + 1;
    }

    public Box getInitBox() {
        return init;
    }
}
